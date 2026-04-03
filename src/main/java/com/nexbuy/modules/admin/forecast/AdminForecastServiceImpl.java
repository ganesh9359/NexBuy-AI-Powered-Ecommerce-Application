package com.nexbuy.modules.admin.forecast;

import com.nexbuy.modules.admin.dto.AdminForecastDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AdminForecastServiceImpl implements AdminForecastService {

    private final JdbcTemplate jdbcTemplate;

    public AdminForecastServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AdminForecastDto getForecast() {
        AdminForecastDto dto = new AdminForecastDto();

        List<AdminForecastDto.ForecastPoint> revenueTrend = buildDailyTrend(true);
        List<AdminForecastDto.ForecastPoint> orderTrend = buildDailyTrend(false);
        List<AdminForecastDto.CategoryForecast> categoryDemand = loadCategoryDemand();
        List<AdminForecastDto.InventoryRisk> inventoryRisks = loadInventoryRisks();

        long recentRevenue = sumTrailing(7, true);
        long previousRevenue = sumWindow(8, 14, true);
        long recentOrders = sumTrailing(7, false);
        long previousOrders = sumWindow(8, 14, false);
        long pendingOrders = queryCount("select count(*) from orders where lower(status) = 'pending'");

        double revenueGrowth = calculateGrowth(previousRevenue, recentRevenue);
        double orderGrowth = calculateGrowth(previousOrders, recentOrders);
        long projectedRevenue = projectForward(recentRevenue, revenueGrowth);
        long projectedOrders = projectForward(recentOrders, orderGrowth);

        dto.setOverview(new AdminForecastDto.Overview(
                recentRevenue,
                recentOrders,
                projectedRevenue,
                projectedOrders,
                revenueGrowth,
                pendingOrders,
                inventoryRisks.size()
        ));
        dto.setRevenueTrend(revenueTrend);
        dto.setOrderTrend(orderTrend);
        dto.setCategoryDemand(categoryDemand);
        dto.setInventoryRisks(inventoryRisks);
        dto.setActions(buildActions(revenueGrowth, pendingOrders, inventoryRisks, categoryDemand));
        return dto;
    }

    private List<AdminForecastDto.ForecastPoint> buildDailyTrend(boolean revenue) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(13);
        Map<LocalDate, Long> points = new LinkedHashMap<>();
        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
            points.put(day, 0L);
        }

        String sql = revenue
                ? """
                        select date(placed_at) as day_label, coalesce(sum(total_cents), 0) as metric_value
                        from orders
                        where lower(status) not in ('cancelled', 'failed')
                          and date(placed_at) >= current_date - interval 13 day
                        group by date(placed_at)
                        order by date(placed_at) asc
                        """
                : """
                        select date(placed_at) as day_label, count(*) as metric_value
                        from orders
                        where lower(status) not in ('cancelled', 'failed')
                          and date(placed_at) >= current_date - interval 13 day
                        group by date(placed_at)
                        order by date(placed_at) asc
                        """;

        jdbcTemplate.query(sql, (org.springframework.jdbc.core.RowCallbackHandler) rs -> points.put(
                rs.getDate("day_label").toLocalDate(),
                rs.getLong("metric_value")
        ));

        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
        return points.entrySet().stream()
                .map(entry -> new AdminForecastDto.ForecastPoint(entry.getKey().format(labelFormatter), entry.getValue()))
                .toList();
    }

    private List<AdminForecastDto.CategoryForecast> loadCategoryDemand() {
        return jdbcTemplate.query(
                """
                        select c.name,
                               c.slug,
                               coalesce(sum(case when o.placed_at >= current_date - interval 29 day then oi.qty else 0 end), 0) as units_sold,
                               coalesce(sum(case when o.placed_at >= current_date - interval 14 day then oi.qty else 0 end), 0) as recent_units,
                               coalesce(sum(case when o.placed_at between current_date - interval 29 day and current_date - interval 15 day then oi.qty else 0 end), 0) as previous_units
                        from order_items oi
                        join orders o on o.id = oi.order_id
                        join product_variants pv on pv.id = oi.variant_id
                        join products p on p.id = pv.product_id
                        join categories c on c.id = p.category_id
                        where lower(o.status) not in ('cancelled', 'failed')
                        group by c.id, c.name, c.slug
                        having units_sold > 0
                        order by units_sold desc, c.name asc
                        limit 6
                        """,
                (rs, rowNum) -> {
                    long recentUnits = rs.getLong("recent_units");
                    long previousUnits = rs.getLong("previous_units");
                    double trendRate = calculateGrowth(previousUnits, recentUnits);
                    long projectedUnits = Math.max(recentUnits, Math.round(recentUnits * (1 + (trendRate / 100.0))));
                    return new AdminForecastDto.CategoryForecast(
                            rs.getString("name"),
                            rs.getString("slug"),
                            rs.getLong("units_sold"),
                            projectedUnits,
                            trendRate
                    );
                }
        );
    }

    private List<AdminForecastDto.InventoryRisk> loadInventoryRisks() {
        return jdbcTemplate.query(
                """
                        select p.title,
                               p.slug,
                               coalesce(i.stock_qty, 0) as stock_qty,
                               coalesce(i.low_stock_threshold, 5) as low_stock_threshold,
                               coalesce(sum(case when o.placed_at >= current_date - interval 29 day and lower(o.status) not in ('cancelled', 'failed') then oi.qty else 0 end), 0) as sold_last_30_days
                        from products p
                        join product_variants pv on pv.product_id = p.id
                        left join inventory i on i.variant_id = pv.id
                        left join order_items oi on oi.variant_id = pv.id
                        left join orders o on o.id = oi.order_id
                        where lower(p.status) = 'active'
                        group by p.id, p.title, p.slug, i.stock_qty, i.low_stock_threshold
                        having stock_qty <= low_stock_threshold or (stock_qty > 0 and sold_last_30_days >= stock_qty)
                        order by sold_last_30_days desc, stock_qty asc, p.title asc
                        limit 8
                        """,
                (rs, rowNum) -> {
                    int stockQty = rs.getInt("stock_qty");
                    int lowStockThreshold = rs.getInt("low_stock_threshold");
                    long soldLast30Days = rs.getLong("sold_last_30_days");
                    String guidance;
                    if (stockQty <= 0) {
                        guidance = "Already out of stock - restock immediately.";
                    } else if (soldLast30Days >= stockQty) {
                        guidance = "Selling faster than remaining stock - reorder this week.";
                    } else if (stockQty <= lowStockThreshold) {
                        guidance = "Near the low-stock threshold - watch closely.";
                    } else {
                        guidance = "Stable, but monitor recent demand.";
                    }
                    return new AdminForecastDto.InventoryRisk(
                            rs.getString("title"),
                            rs.getString("slug"),
                            stockQty,
                            lowStockThreshold,
                            soldLast30Days,
                            guidance
                    );
                }
        );
    }

    private List<String> buildActions(double revenueGrowth,
                                      long pendingOrders,
                                      List<AdminForecastDto.InventoryRisk> inventoryRisks,
                                      List<AdminForecastDto.CategoryForecast> categoryDemand) {
        List<String> actions = new ArrayList<>();
        if (revenueGrowth < 0) {
            actions.add("Revenue cooled versus the previous week, so lead the storefront with sharper promotions and recommendations.");
        } else {
            actions.add("Demand is moving upward, so keep top categories visible and protect stock on the best sellers.");
        }
        if (pendingOrders > 0) {
            actions.add(pendingOrders + " orders are still pending - moving them faster into shipped status will protect customer confidence.");
        }
        if (!inventoryRisks.isEmpty()) {
            actions.add("There are " + inventoryRisks.size() + " inventory risks worth reviewing before the next demand spike.");
        }
        if (!categoryDemand.isEmpty()) {
            actions.add(categoryDemand.get(0).category() + " is the strongest demand lane right now and deserves extra merchandising focus.");
        }
        return actions;
    }

    private long sumTrailing(int trailingDays, boolean revenue) {
        String metric = revenue ? "coalesce(sum(total_cents), 0)" : "count(*)";
        Number number = jdbcTemplate.queryForObject(
                "select " + metric + " from orders where lower(status) not in ('cancelled', 'failed') and placed_at >= current_timestamp - interval ? day",
                Number.class,
                trailingDays
        );
        return number == null ? 0L : number.longValue();
    }

    private long sumWindow(int startDaysAgo, int endDaysAgo, boolean revenue) {
        String metric = revenue ? "coalesce(sum(total_cents), 0)" : "count(*)";
        Number number = jdbcTemplate.queryForObject(
                "select " + metric + " from orders where lower(status) not in ('cancelled', 'failed') and placed_at >= current_timestamp - interval ? day and placed_at < current_timestamp - interval ? day",
                Number.class,
                endDaysAgo,
                startDaysAgo - 1
        );
        return number == null ? 0L : number.longValue();
    }

    private double calculateGrowth(long previous, long current) {
        if (previous <= 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((current - previous) * 100.0) / previous;
    }

    private long projectForward(long trailingValue, double growthRate) {
        double factor = 1.0 + Math.max(-0.25, Math.min(0.35, growthRate / 100.0));
        return Math.max(0L, Math.round(trailingValue * factor));
    }

    private long queryCount(String sql) {
        Number count = jdbcTemplate.queryForObject(sql, Number.class);
        return count == null ? 0L : count.longValue();
    }
}