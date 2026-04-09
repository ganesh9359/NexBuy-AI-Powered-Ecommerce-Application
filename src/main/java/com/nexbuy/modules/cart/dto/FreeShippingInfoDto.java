package com.nexbuy.modules.cart.dto;

public class FreeShippingInfoDto {
    private int thresholdCents; // 200000 = 2000 rupees
    private int currentSubtotalCents;
    private int amountNeededCents; // Amount remaining to reach threshold
    private boolean isEligible; // true if currentSubtotal >= threshold
    private double progressPercentage; // 0-100
    private String formattedThreshold;
    private String formattedCurrentTotal;
    private String formattedAmountNeeded;

    public FreeShippingInfoDto(int thresholdCents, int currentSubtotalCents) {
        this.thresholdCents = thresholdCents;
        this.currentSubtotalCents = currentSubtotalCents;
        this.isEligible = currentSubtotalCents >= thresholdCents;
        this.amountNeededCents = Math.max(0, thresholdCents - currentSubtotalCents);
        this.progressPercentage = currentSubtotalCents >= thresholdCents ? 100 : 
                                  (double) currentSubtotalCents / thresholdCents * 100;
        
        this.formattedThreshold = formatAmount(thresholdCents);
        this.formattedCurrentTotal = formatAmount(currentSubtotalCents);
        this.formattedAmountNeeded = formatAmount(amountNeededCents);
    }

    private String formatAmount(int cents) {
        return "₹" + String.format("%.0f", cents / 100.0);
    }

    public int getThresholdCents() {
        return thresholdCents;
    }

    public int getCurrentSubtotalCents() {
        return currentSubtotalCents;
    }

    public int getAmountNeededCents() {
        return amountNeededCents;
    }

    public boolean isEligible() {
        return isEligible;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public String getFormattedThreshold() {
        return formattedThreshold;
    }

    public String getFormattedCurrentTotal() {
        return formattedCurrentTotal;
    }

    public String getFormattedAmountNeeded() {
        return formattedAmountNeeded;
    }
}
