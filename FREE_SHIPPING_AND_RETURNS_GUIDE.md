# Free Shipping & Return Management Feature Documentation

## Overview
This document describes the implementation of the free shipping threshold and return request management system in NexBuy.

---

## 1. FREE SHIPPING FEATURE

### Backend Implementation

#### Constants
- **Location**: `CommerceSupport.java`
- **Threshold**: ₹2,000 (200,000 paise/cents)
- **Shipping Charge**: ₹199 (19,900 paise/cents)
- **Logic**: Free shipping is applied when cart subtotal ≥ ₹2,000

#### Updated Data Transfer Objects (DTOs)

**FreeShippingInfoDto.java**
```java
- thresholdCents: int (200000)
- currentSubtotalCents: int
- amountNeededCents: int (max(0, threshold - current))
- isEligible: boolean (current >= threshold)
- progressPercentage: double (0-100)
- formattedThreshold: String ("₹2,000")
- formattedCurrentTotal: String ("₹1,500")
- formattedAmountNeeded: String ("₹500")
```

**CartItemDto.CartTotals** (Updated)
```java
// Added new field:
FreeShippingInfoDto freeShippingInfo
```

#### Cart Calculation Logic
**Method**: `CommerceSupport.calculateTotals()`
```java
int shipping = subtotal == 0 ? 0 : subtotal >= 200000 ? 0 : 19900;
FreeShippingInfoDto freeShippingInfo = new FreeShippingInfoDto(200000, subtotal);
return new CartTotals(..., freeShippingInfo);
```

### Frontend Implementation

#### Updated Cart Service (cart.service.ts)
Added interfaces:
- `FreeShippingInfo` - Data structure for free shipping information
- Updated `CartTotals` - Now includes optional `freeShippingInfo` field

#### Cart Component UI (cart-home.component.html)
**Free Shipping Section Features**:
1. **Progress Bar Display**
   - Visual progress from ₹0 to ₹2,000
   - Animated fill with gradient from yellow (#fff176) to (#ffeb3b)
   - Current amount / Threshold displayed above bar

2. **Status Messages**
   - **Locked**: "Add ₹500 more to unlock free shipping!"
   - **Unlocked**: "🎊 Congratulations! You've unlocked free shipping on this order!"

3. **Unlock Badge**
   - Appears when cart reaches ₹2,000
   - Text: "🎉 UNLOCKED!"
   - Animated pulse effect

4. **Styling**
   - Purple gradient background (locked state)
   - Green gradient background (unlocked state)
   - Responsive design for mobile devices
   - Light theme support

#### Animations (cart-home.component.ts)
```typescript
trigger('animateUnlock', [
  transition(':enter', [
    style({ opacity: 0, scale: 0.8 }),
    animate('600ms cubic-bezier(0.34, 1.56, 0.64, 1)')
  ])
])

trigger('progressAnimation', [
  transition(':enter', [
    style({ width: '0%' }),
    animate('1200ms cubic-bezier(0.25, 0.46, 0.45, 0.94)')
  ])
])
```

#### Styling (cart-home.component.scss)
- Purple & gradient backgrounds
- Smooth progress bar animation
- Responsive breakpoints for mobile (640px)
- Pulsing badge animation on unlock
- Box shadow for depth effect

---

## 2. RETURN REQUEST MANAGEMENT

### Database Schema

#### Tables Created

**return_requests**
```sql
id BIGINT PRIMARY KEY AUTO_INCREMENT
order_id BIGINT NOT NULL UNIQUE (FK → orders.id)
status ENUM('requested','approved','accepted','rejected','completed','cancelled')
refund_status ENUM('not_started','pending','processing','processed','failed','cancelled')
reason TEXT
requested_at DATETIME (auto-set to CURRENT_TIMESTAMP)
reviewed_at DATETIME (set when admin approves/rejects)
picked_at DATETIME (set when pickup is confirmed)
updated_at DATETIME (auto-updated on changes)
```

**refunds**
```sql
id BIGINT PRIMARY KEY AUTO_INCREMENT
order_id BIGINT NOT NULL UNIQUE (FK → orders.id)
payment_id BIGINT (FK → payments.id)
amount_cents INT
currency VARCHAR(3) - DEFAULT 'INR'
status ENUM('pending','processing','processed','failed','cancelled')
provider_refund_id VARCHAR(255) - Payment gateway refund ID
note TEXT
requested_at DATETIME (auto-set to CURRENT_TIMESTAMP)
processed_at DATETIME (set when refund completes)
updated_at DATETIME (auto-updated on changes)
```

**Indexes Created**:
- idx_refunds_order: (order_id)
- idx_refunds_status: (status)
- idx_return_requests_order: (order_id)
- idx_return_requests_status: (status)

### Backend APIs

#### Admin Service Methods

**1. Get All Return Requests**
```java
GET /admin/returns
Returns: List<ReturnRequestDto>

Response includes:
- id, orderId, customerId, orderNumber
- status (requested|approved|accepted|rejected|completed|cancelled)
- refundStatus (not_started|pending|processing|processed|failed|cancelled)
- reason, refundAmountInr
- requestedAt, reviewedAt, pickedAt, updatedAt
```

**2. Get Single Return Request**
```java
GET /admin/returns/{returnRequestId}
Returns: ReturnRequestDto
```

**3. Update Return Request**
```java
PATCH /admin/returns/{returnRequestId}
Body: AdminReturnUpdateRequest
  - action: string (approve|accept|reject|complete|cancel)
  - note: string (optional)

Flow by Action:
- approve: Sets status='approved', initiates refund
- accept: Sets status='accepted', marks picked_at timestamp
- reject: Sets status='rejected', reviewed_at timestamp
- complete: Sets status='completed'
- cancel: Sets status='cancelled'
```

**4. Get Refund Status**
```java
GET /admin/orders/{orderId}/refund-status
Returns: RefundStatusDto

Response:
- id, orderId, paymentId
- amountCents, currency
- status, providerRefundId
- note, requestedAt, processedAt, updatedAt
```

### Return Flow Workflow

```
1. CUSTOMER REQUESTS RETURN
   ├─ Status: requested
   ├─ Refund Status: not_started
   └─ User provides reason

2. ADMIN APPROVES RETURN
   ├─ Status: approved
   ├─ Refund Status: processing
   ├─ Action: Initiates payment gateway refund
   └─ Notification sent to customer

3. PICKUP SCHEDULED & CONFIRMED
   ├─ Status: accepted (can be set separately)
   ├─ picked_at: Timestamp recorded
   └─ Tracking info shared with customer

4. REFUND PROCESSED
   ├─ Refund Status: processed
   ├─ provider_refund_id: Payment gateway confirmation ID
   ├─ processed_at: Timestamp recorded
   └─ Notification: Refund amount credited

5. RETURN COMPLETED
   ├─ Status: completed
   ├─ Customer receives returned item at warehouse
   └─ Final status recorded
```

### Return Request DTOs

**ReturnRequestDto**
```java
id: Long
orderId: Long
customerId: Long
orderNumber: String
status: String
refundStatus: String
reason: String
refundAmountInr: Double
requestedAt: LocalDateTime
reviewedAt: LocalDateTime
pickedAt: LocalDateTime
updatedAt: LocalDateTime
```

**AdminReturnUpdateRequest**
```java
action: String (approve|accept|reject|complete|cancel)
note: String (optional)
```

**RefundStatusDto**
```java
id: Long
orderId: Long
paymentId: Long
amountCents: Integer
currency: String
status: String (pending|processing|processed|failed|cancelled)
providerRefundId: String
note: String
requestedAt: LocalDateTime
processedAt: LocalDateTime
updatedAt: LocalDateTime
```

---

## 3. FRONTEND INTEGRATION

### Order Detail Component
The existing `order-detail.component` already displays:
- Return request status (if exists)
- Refund information with status badges
- Timeline of events
- Return and cancel buttons

### Admin Dashboard (To Be Implemented)
Create `/admin/returns` page to display:
- List of all return requests
- Filter by status (requested, approved, accepted, etc.)
- Search by order number / customer
- Bulk actions (approve, reject, mark as picked)
- Inline editing for status updates

---

## 4. CONFIGURATION

### Free Shipping Configuration
To change the free shipping threshold, update `CommerceSupport.java`:
```java
private static final int FREE_SHIPPING_THRESHOLD_CENTS = 200000; // Change this value
```

### Shipping Charge
```java
private static final int SHIPPING_CENTS = 19900; // ₹199
```

---

## 5. TESTING

### Database Setup
Run the migration script:
```sql
-- Apply schema updates (in schema.sql)
-- Load test data (in seed_returns.sql)
```

### Backend Testing
1. Create a cart with items < ₹2,000
   - Verify `freeShippingInfo.isEligible = false`
   - Verify `shippingCents = 19900`
   - Verify `amountNeededCents = calculated_correctly`

2. Add items to reach ₹2,000
   - Verify `freeShippingInfo.isEligible = true`
   - Verify `shippingCents = 0`
   - Verify `amountNeededCents = 0`

3. Test return request flow
   - Create return request via OrderService
   - Approve return via AdminService
   - Verify refund created in database
   - Verify refund status changes to 'processing'

### Frontend Testing
1. View cart with items < ₹2,000
   - Purple gradient card displays
   - Progress bar shows progress
   - Message shows amount needed: "Add ₹500 more..."

2. Add items to reach ₹2,000
   - Card changes to green gradient
   - Progress bar fills to 100%
   - Badge appears with animation
   - Success message displays

3. Mobile responsiveness
   - Card adapts to small screens
   - Progress bar remains visible
   - Text scales appropriately

---

## 6. ENVIRONMENT VARIABLES

No additional environment variables required.
All configuration through Java constants and database.

---

## 7. MIGRATION NOTES

### Breaking Changes
None. The `CartTotals` DTO now includes an optional `freeShippingInfo` field, which is backward compatible.

### Data Migration
1. Existing orders unaffected
2. New `return_requests` and `refunds` tables initially empty
3. Existing carts will show new free shipping indicator

---

## 8. API ENDPOINTS SUMMARY

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/admin/returns` | List all return requests |
| GET | `/admin/returns/{id}` | Get single return request |
| PATCH | `/admin/returns/{id}` | Update return status |
| GET | `/admin/orders/{orderId}/refund-status` | Get refund information |

---

## 9. USER MESSAGES

### In Cart
- "Add ₹500 more to unlock free shipping!"
- "🎊 Congratulations! You've unlocked free shipping on this order!"

### In Order Status
- Return request status updates
- Refund processing status
- Timeline events for returns and refunds

---

## 10. NEXT STEPS

1. **Admin Dashboard**: Create UI for return management
2. **Email Notifications**: Send updates when return approaches
3. **Return Window UI**: Show countdown timer for return window (7 days)
4. **Bulk Operations**: Admin bulk approve/reject returns
5. **Analytics**: Track return rate and popular return reasons
6. **Pickup Integration**: Connect with shipping partners for pickup scheduling
