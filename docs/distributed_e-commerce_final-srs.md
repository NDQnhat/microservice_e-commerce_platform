# STANDALONE SOFTWARE REQUIREMENTS SPECIFICATION
## Distributed E-commerce & Fulfillment Platform

## Document Control

| Field | Value |
|---|---|
| Document | STANDALONE_SRS |
| Version | 3.0 (Standalone, self-contained) |
| Status | DRAFT — contains explicit `NEEDS_CLARIFICATION`, `TECH_STACK_DECISION_REQUIRED`, and `BLOCKING` items registered in Section 20 |
| Predecessor Artifacts | Business Analysis Artifact v2.0, System Design Artifact v1.0, FINAL_SRS v1.0 — **all content required for implementation has been merged into this document; none of those documents need to be consulted separately.** |
| Audience | System Design Agent, Code Generation Agent, Tech Stack Decision Agent |
| Source of Truth Policy | Business Requirement > System Design Interpretation > Assumption > Recommendation. Technology is decided later by a Tech Stack Agent using a `TECH_STACK_TEMPLATE`; this document never selects technology. |

---

## 1. Introduction

### 1.1 Purpose
This document is the single, self-contained specification of a distributed e-commerce and fulfillment platform built as a personal portfolio project. It fully defines business behavior, domain rules, lifecycle/state machines, data structures (logical), API behavior, internal event contracts, distributed-system guarantees, security, and non-functional requirements. Any implementer (human or AI) needs **only this document** to begin system design and code generation. Technology selection (language, framework, database engine, message broker, cache, auth library, etc.) is intentionally deferred and marked `TECH_STACK_DECISION_REQUIRED`.

### 1.2 Scope
**In scope (MVP):** customer registration/login; customer address book; product/category/SKU/variant/attribute/media catalog; pricing (base + time-limited promotional price) with price history; shopping cart; checkout with inventory reservation, monetary breakdown, and item/address snapshotting; simulated payment processing; order tracking with full timeline; customer-initiated order cancellation (within allowed window); simulated fulfillment/shipping; notification (template-based, with delivery log); full back-office/admin capability across 5 RBAC roles (Super Admin, Catalog Admin, Inventory Admin, Order Operations Admin, Customer Support) covering: dashboard, catalog/pricing/inventory management, inventory audit trail, order management with enforced state machine, payment reconciliation, fulfillment management, customer support assisted actions, RBAC administration, audit log, notification template/delivery management, business configuration, exception/failed-transaction management. The system must exhibit distributed-system behaviors: Saga/compensation, transactional outbox, idempotency, eventual consistency, retry, timeout, circuit breaker, distributed locking, service discovery, load balancing, and observability (correlation tracing).

**Out of scope for MVP (explicitly deferred, not to be implemented now):**
| Capability | Classification | Rationale |
|---|---|---|
| Coupon / Promotion codes | Phase 2 | Not required for MVP distributed-system demonstration |
| Return / Refund (post-delivery reverse flow) | Phase 2 | Requires additional order states not defined in this MVP state machine |
| Review / Rating | Phase 3 (or dropped) | Pure content feature, unrelated to consistency/invariant goals |
| Multi-warehouse inventory allocation | Phase 3 | Out of scope; single inventory source assumed (ASM-002) |
| Marketplace / multi-seller model | Permanently out of scope | Fundamentally changes domain model |
| Multi-currency / multi-locale | Out of scope | Not required (ASM-001) |
| Real payment processing with real money | Out of scope | Payment Gateway is simulated (ASM-003) |
| Real third-party carrier integration | Out of scope | Shipping is simulated internally (ASM-004) |
| Native mobile app, live chat support | Out of scope | Not requested |
| Tax calculation in Grand Total | Out of scope for MVP | ASM-008; see Decision Register CQ-012 |

### 1.3 Definitions, Acronyms, Abbreviations
| Term | Meaning |
|---|---|
| SKU | Stock Keeping Unit — a sellable variant of a Product; the unit at which inventory is tracked |
| RBAC | Role-Based Access Control |
| Saga | Distributed transaction coordination pattern using a sequence of steps with compensating actions |
| Outbox | Transactional Outbox — a pattern guaranteeing that an event is published if and only if the associated state change was committed |
| TTL | Time To Live |
| Snapshot | An immutable copy of data captured at a specific transaction moment; not affected by later changes to the source data |
| Idempotency Key | A client-supplied token that lets the system recognize and safely handle a duplicate submission of the same logical operation |
| Deny-by-default | A newly created role/permission grants no access until explicitly assigned |
| Append-only | A dataset that supports only insert operations; no update or delete operation exists for it in the system |
| Actor | A human role, system process, or external system capable of initiating an action in this specification |

### 1.4 References
None. This document is self-contained. No external Business Analysis Artifact, System Design Artifact, or other document is required to understand or implement the requirements herein.

---

## 2. Overall Description

### 2.1 Product Perspective
A new, standalone system (not integrated with any pre-existing enterprise system), architected as a set of cooperating logical services (see Section 3) that communicate synchronously (request/response) and asynchronously (event-driven). The system serves both customer-facing commerce flows and an internal back-office/admin flow operated by staff with differentiated, least-privilege roles.

### 2.2 Product Functions (summary)
- **Customer-facing:** registration, login, address book management, product browsing/search, product/SKU detail viewing, cart management, checkout, payment, order tracking (with full timeline), order cancellation (within allowed window), receiving notifications.
- **Back-office (RBAC, 5 roles):** business dashboard; product/category/SKU/variant/attribute/media management; pricing management; inventory management and inventory audit trail; order management and order timeline; payment reconciliation; fulfillment/shipping management; customer support assisted actions; RBAC/role/permission management; audit log query; notification template and delivery log management; business configuration management; exception/failed-transaction board.
- **System-internal (automated actors):** inventory reservation and release/compensation; monetary breakdown calculation; order item and address snapshotting; order timeline recording; notification dispatch; stuck-order detection.

### 2.3 User Classes and Characteristics

| Actor ID | Actor | Type | Characteristics |
|---|---|---|---|
| ACT-001 | Guest | Human, unauthenticated | Can browse/search products only; cannot check out (see BR-009 / Decision Register CQ-001) |
| ACT-002 | Registered Customer | Human, authenticated | Performs the full purchase lifecycle; owns exactly the data scoped to their own account |
| ACT-003 | Back-office User | Human, authenticated, internal | Generic container for staff accounts; actual capability is determined entirely by assigned Role(s) (see 2.3.1) |
| ACT-004 | System Scheduler / Background Process | Non-human, internal | Executes time-based and event-based automated logic (reservation expiry, compensation, timeline recording, breakdown computation, stuck-order detection) |
| ACT-005 | Payment Gateway (simulated) | Non-human, external, simulated | Sends asynchronous payment result callbacks to the platform |
| ACT-006 | Notification Channel (simulated) | Non-human, external, simulated | Receives dispatch requests and reports delivery status |

#### 2.3.1 Back-office Roles (RBAC)

| Role ID | Role Name | Capability Scope | Least-Privilege Boundary |
|---|---|---|---|
| ROLE-001 | Super Admin | RBAC administration, Business Configuration, Audit Log query, user account lock/unlock | Does not bypass Order/Inventory/Payment state machines under any circumstance (BR-011) |
| ROLE-002 | Catalog Admin | Product, Category, SKU/Variant, Attribute, Media, Pricing management | No access to Order, Payment, Inventory-adjustment, or RBAC resources |
| ROLE-003 | Inventory Admin | Inventory adjustment, Inventory Audit Trail | No access to pricing or order cancellation |
| ROLE-004 | Order Operations Admin | Order management, Order Timeline, Fulfillment/Shipping management, Payment Reconciliation (view + controlled trigger), Exception Board, Stuck-Order detection/escalation, Dashboard | No access to Product/Pricing/RBAC administration |
| ROLE-005 | Customer Support | Customer profile/order read access; whitelist of assisted actions only | Cannot alter monetary values of an order; cannot alter inventory directly (BR-016) |

A back-office user account may be assigned more than one Role. Role↔Permission and User↔Role are both many-to-many relationships (see Section 11).

### 2.4 Operating Environment
The system is a distributed system composed of multiple cooperating logical services, communicating via synchronous request/response calls and asynchronous event messages, with a caching/locking layer and containerized deployment with a CI/CD pipeline. **Concrete technology for each of these mechanisms is `TECH_STACK_DECISION_REQUIRED`** (see Section 2.5); this section specifies only the required behavior, not the implementing technology.

### 2.5 Design and Implementation Constraints

The following constraints on technology **have already been confirmed** and must be respected once technology is selected by the Tech Stack Agent:

| ID | Constraint |
|---|---|
| CON-001 | The customer/back-office user interface layer must be built using the React component model (with Next.js as the surrounding framework). |
| CON-002 | The backend must be organized as a microservices architecture, implemented using the Spring Boot framework. |
| CON-003 | Synchronous inter-service communication must use REST over HTTP. |
| CON-004 | Asynchronous inter-service communication must use Apache Kafka as the event backbone. |
| CON-005 | A Redis-compatible in-memory data store must be used for caching, cart storage, distributed locking, and/or inventory reservation support, wherever those needs arise. |
| CON-006 | Each logical service should own its own database when practical (database-per-service principle); shared default schemas across services are disallowed. |
| CON-007 | All deployable services must be containerized (Docker-compatible container images). |
| CON-008 | A CI/CD pipeline (build/test/deploy) must exist for every service. |
| CON-009 | Every architectural/technology decision must have a clear engineering justification tied to a real business or technical problem; nothing may be added purely for résumé value. |

The following remain **technology decisions not yet made** and must not be invented by this document, by System Design, or by Code Generation without explicit Tech Stack Agent input:

| Concern | Status |
|---|---|
| Concrete database engine per service | `TECH_STACK_DECISION_REQUIRED` |
| ORM / persistence framework | `TECH_STACK_DECISION_REQUIRED` |
| Authentication token mechanism (JWT vs. opaque/session, library) | `TECH_STACK_DECISION_REQUIRED` |
| Payment Gateway callback authentication mechanism (signature, shared secret, mTLS) | `TECH_STACK_DECISION_REQUIRED` |
| Media/file storage mechanism | `TECH_STACK_DECISION_REQUIRED` |
| Testing framework/tools | `TECH_STACK_DECISION_REQUIRED` |
| CI/CD platform | `TECH_STACK_DECISION_REQUIRED` |
| Password hashing algorithm (specific one-way function) | `TECH_STACK_DECISION_REQUIRED` — requirement is "must not be reversible / must not be stored plaintext" (NFR-SEC-001); algorithm choice deferred |

### 2.6 Assumptions and Dependencies

**Assumptions** (non-binding; must not be treated as confirmed requirements by any downstream agent):

| ID | Assumption |
|---|---|
| ASM-001 | Single currency / single locale / single language operation |
| ASM-002 | Single warehouse / single inventory source (no multi-warehouse allocation in MVP) |
| ASM-003 | The Payment Gateway is a simulation, not a real financial processor |
| ASM-004 | Shipping/delivery is simulated internally; no real carrier integration |
| ASM-005 | Only the Customer actor class and the five defined back-office roles exist; no Seller role |
| ASM-006 | Checkout requires authentication (no guest checkout) — pending confirmation, see Decision Register CQ-001 |
| ASM-007 | A Product that declares no variant automatically receives exactly one default SKU — pending confirmation, see Decision Register CQ-011 |
| ASM-008 | Grand Total does not include tax in the MVP — pending confirmation, see Decision Register CQ-012 |

**External Dependencies** (logical, not technology-bound):

| ID | Dependency | Nature |
|---|---|---|
| EXT-001 | An asynchronous message backbone (event bus) | Required for all Kafka-designated flows (CON-004 confirms the concrete technology) |
| EXT-002 | An in-memory data store supporting caching, cart state, and distributed locking with TTL | CON-005 confirms Redis as the technology |
| EXT-003 | A Payment Gateway, simulated, reachable via outbound request and inbound callback | ASM-003 |
| EXT-004 | A Notification Channel, simulated; concrete channel type (email/SMS/in-app/push) — **`NEEDS_CLARIFICATION`**, see Decision Register CQ-006 |
| EXT-005 | A service discovery mechanism allowing services to locate one another dynamically | CON-002/CON-003 imply this is required for the microservices architecture to function; concrete technology `TECH_STACK_DECISION_REQUIRED` |
| EXT-006 | A fulfillment/shipping simulation capability, internal to the system | ASM-004 |

---

## 3. Logical Service Decomposition (non-binding on physical deployment)

The system's capabilities are grouped into 12 logical services, decomposed by **business capability**, not by actor/role (role/permission is an authorization concern layered on top of these services, not a service-boundary concern). A System Design Agent may deploy these as separate physical services or combine some of them; the only binding constraint is CON-006 (database-per-service when practical) and the requirement that every FR below be fully implemented by some component.

| Logical Service | Business Capability | Owns (Entities) | Implements (FR) |
|---|---|---|---|
| Identity & Access | Registration, login, RBAC, address book | user_account, customer_address, role, permission, role_permission, user_role | FR-001, FR-002, FR-018, FR-022, FR-033 |
| Catalog | Product/Category/SKU/Variant/Attribute/Media | category, product, product_attribute, product_attribute_value, sku, sku_attribute_value, product_media | FR-003, FR-004, FR-005, FR-014, FR-015, FR-023, FR-024, FR-025 |
| Pricing | Base price, promotional price, price history | price, price_promotion | FR-026 |
| Cart | Shopping cart | cart, cart_item | FR-006, FR-007, FR-008 |
| Inventory | Stock levels, reservation, adjustment, audit trail | inventory, inventory_reservation, inventory_adjustment_log | FR-016, FR-019, FR-020, FR-030, FR-031 |
| Order | Order lifecycle, checkout orchestration, monetary breakdown, item snapshot, timeline, admin order operations, stuck-order detection | orders, order_item, order_timeline_event | FR-009, FR-011, FR-012, FR-017, FR-021, FR-027, FR-028, FR-029, FR-042 |
| Payment | Payment processing, reconciliation | payment_transaction | FR-010, FR-039 |
| Fulfillment / Shipping | Simulated packing/shipping | shipment | FR-040 |
| Notification | Template management, dispatch, delivery log | notification_template, notification_log | FR-013, FR-035, FR-036 |
| Audit & Compliance | Aggregated sensitive-action audit log | audit_log | FR-034 |
| Business Configuration | Time-versioned business parameters | business_configuration | FR-037 |
| Exception Management | Aggregated exception/failed-transaction read model | exception_record | FR-038, part of FR-032 |

**Communication requirement:** Order Service must call Inventory Service and Pricing Service synchronously during checkout (to reserve stock and to obtain the effective price to snapshot). Order Service, Inventory Service, and Payment Service must publish state-change events asynchronously so that Notification, Fulfillment, Audit & Compliance, and Exception Management services can react without being on the synchronous request path (this is what enables NFR-FAULTISO-001 — read-path independence from Order Service availability).

---

## 4. Functional Requirements

> Each FR below is complete: Description, Actor, Preconditions, Trigger, Main Flow, Alternative Flow, Exception Flow, Postconditions, Business Rules, Acceptance Criteria (Gherkin), Traceability. This is the authoritative behavior specification; no external document supplements it.

**FR-001 — Register Customer Account**
- Description: Allows a person without an account to create a new Customer account.
- Actor: ACT-001
- Preconditions: The supplied email/username does not already exist in the system.
- Trigger: Registration form submission.
- Main Flow: Validate input → create `user_account` with `account_type = CUSTOMER`, `status = ACTIVE` → return confirmation.
- Alternative Flow: None.
- Exception Flow: Email already registered → `CONFLICT` (409, error code `CONFLICT`); invalid input (format, missing fields, weak password) → `VALIDATION_ERROR` (400).
- Postconditions: New account exists in `ACTIVE` status.
- Business Rules: NFR-SEC-001 (password must never be stored in plaintext).
- Acceptance Criteria:
```gherkin
Feature: Customer Registration
Scenario: Successful registration
  Given no account exists with the given email
  When the person submits valid registration data
  Then a new account is created
  And the account status is ACTIVE

Scenario: Duplicate email
  Given an account already exists with the given email
  When the person submits a registration request with that email
  Then the system rejects the request with CONFLICT
  And no new account is created

Scenario: Weak/invalid input
  Given the submitted password does not meet the minimum validation rule
  When the person submits the registration request
  Then the system rejects with VALIDATION_ERROR
```
- Traceability: US-001 | Entity: user_account | API: API-IAM-001 | NFR: NFR-SEC-001

---

**FR-002 — Authenticate (Login)**
- Description: Authenticates a Customer or Back-office User and issues an authenticated session.
- Actor: ACT-002 / ACT-003
- Preconditions: Account exists and `status = ACTIVE`.
- Trigger: Login credential submission.
- Main Flow: Validate credentials → issue an access token (mechanism `TECH_STACK_DECISION_REQUIRED`) carrying the account's role(s).
- Alternative Flow: None.
- Exception Flow: Invalid credentials → `AUTHENTICATION_FAILED` (401); account `status = LOCKED` → `AUTHORIZATION_FAILED` (403, reason "account locked").
- Postconditions: Caller holds a valid session usable for subsequent authenticated requests.
- Business Rules: BR-009 (checkout requires an authenticated session — enforced later at checkout time, not at login).
- Acceptance Criteria:
```gherkin
Feature: Login
Scenario: Successful login
  Given a valid ACTIVE account
  When correct credentials are submitted
  Then an authenticated session is issued containing the account's role(s)

Scenario: Wrong credentials
  Given an account exists
  When incorrect credentials are submitted
  Then the system returns AUTHENTICATION_FAILED
  And no session is issued

Scenario: Locked account
  Given the account status is LOCKED
  When correct credentials are submitted
  Then the system rejects the login with AUTHORIZATION_FAILED
```
- Traceability: US-002 | Entity: user_account | API: API-IAM-002 | BR: BR-009 | NFR: NFR-SEC-001, NFR-SEC-002

---

**FR-003 — Browse Products by Category**
- Description: Lists products belonging to a category.
- Actor: ACT-001 / ACT-002
- Preconditions: Category exists.
- Trigger: Category selection.
- Main Flow: Query active products under the category → return paginated result.
- Alternative Flow: Category has zero active products → return empty result set (not an error).
- Exception Flow: Category does not exist → `NOT_FOUND` (404).
- Postconditions: Read-only; no state change.
- Business Rules: None.
- Acceptance Criteria: happy path (products returned), empty category (empty list returned), not-found category.
- Traceability: US-003 | Entity: category, product | API: API-CAT-001 | NFR: NFR-PERF-001

---

**FR-004 — Search Products**
- Description: Finds products matching a keyword.
- Actor: ACT-001 / ACT-002
- Preconditions: None.
- Trigger: Search query submission.
- Main Flow: Query catalog by keyword → return matching results.
- Alternative Flow: None.
- Exception Flow: No matches → return an empty result set (HTTP 200, not an error).
- Postconditions: Read-only.
- Business Rules: None.
- Acceptance Criteria: has-results and no-results scenarios.
- Traceability: US-004 | Entity: product | API: API-CAT-002 | NFR: NFR-PERF-001

---

**FR-005 — View Product/SKU Detail**
- Description: Displays full product detail, its SKU/variant list, effective price, and per-SKU available stock.
- Actor: ACT-001 / ACT-002
- Preconditions: Product/SKU exists.
- Trigger: Open product detail page.
- Main Flow: Return product info + SKU list + effective price per SKU (computed per FR-026's "effective price" rule) + available-stock status per SKU.
- Alternative Flow: None.
- Exception Flow: Product not found or removed → `NOT_FOUND` (404).
- Postconditions: Read-only.
- Business Rules: BR-004, BR-014.
- Acceptance Criteria: in-stock detail view; not-found scenario.
- Traceability: US-005 | Entity: product, sku, inventory | API: API-CAT-003 | BR: BR-004, BR-014 | NFR: NFR-PERF-001

---

**FR-006 — Add SKU to Cart**
- Description: Adds a specific SKU with a quantity to the customer's cart.
- Actor: ACT-002
- Preconditions: Authenticated; requested quantity ≤ the SKU's currently available stock (on-hand minus already-reserved, see Section 11 `inventory`).
- Trigger: "Add to cart" action.
- Main Flow: Verify available stock ≥ requested quantity → add or merge line into `cart_item`.
- Alternative Flow: SKU already in cart → increment quantity on the existing line rather than creating a duplicate line.
- Exception Flow: Requested quantity exceeds available stock → `BUSINESS_RULE_VIOLATION` (422, rule BR-004), with the currently available quantity communicated in the error detail; SKU does not exist → `NOT_FOUND` (404).
- Postconditions: Cart reflects new/updated line.
- Business Rules: BR-004.
- Acceptance Criteria: successful add; over-limit rejection.
- Traceability: US-006 | Entity: cart_item, sku | API: API-CART-001 | BR: BR-004

---

**FR-007 — Update / Remove Cart Item**
- Description: Change quantity of, or remove, a cart line.
- Actor: ACT-002
- Preconditions: Cart line exists and belongs to the requesting customer.
- Trigger: Edit or remove action on a cart line.
- Main Flow: Update quantity (bounded by available stock) or delete the line.
- Alternative Flow: Full removal.
- Exception Flow: New quantity exceeds available stock → `BUSINESS_RULE_VIOLATION` (422, BR-004).
- Postconditions: Cart state updated.
- Business Rules: BR-004.
- Traceability: US-007 | Entity: cart_item | API: API-CART-002 | BR: BR-004

---

**FR-008 — View Cart**
- Description: Shows current cart contents and subtotal.
- Actor: ACT-002
- Preconditions: None.
- Trigger: Open cart page.
- Main Flow: Return all cart lines, quantities, and computed subtotal.
- Alternative Flow: Empty cart → show empty state.
- Exception Flow: None.
- Postconditions: Read-only.
- Business Rules: None.
- Traceability: US-008 | Entity: cart, cart_item | API: API-CART-003

---

**FR-009 — Checkout (Create Order)**
- Description: Converts the customer's cart into an order: reserves inventory, snapshots the shipping address, computes the monetary breakdown, snapshots each order line, and creates the order in its initial lifecycle state.
- Actor: ACT-002
- Preconditions: Customer authenticated; cart is non-empty; a shipping address has been selected; every SKU in the cart has available stock ≥ its cart quantity at the moment of confirmation.
- Trigger: Customer confirms checkout, supplying a client-generated idempotency key.
- Main Flow:
  1. Validate cart is non-empty and address is selected.
  2. For every SKU in the cart, atomically attempt to reserve the requested quantity (see FR-019). If all reservations succeed, proceed; if the request as a whole cannot be fully reserved, no partial order is created.
  3. Snapshot the selected shipping address fields into the order (BR-017).
  4. Determine each SKU's effective price at this instant (FR-026's effective-price rule) and snapshot product name, SKU code, attribute description, and unit price into each order line (FR-028, BR-013).
  5. Compute Subtotal, Shipping Fee, Discount (= 0 per ASM-008 in MVP), and Grand Total = Subtotal + Shipping Fee − Discount (FR-027).
  6. Create the order in state `RESERVED`.
  7. Record the first Order Timeline entry (`from_status = null`, `to_status = RESERVED`, `actor_type = CUSTOMER`).
  8. Publish the `OrderCreated` event (Section 13) using the transactional-outbox guarantee (Section 14).
  9. Return the created order (id, status, breakdown, snapshotted items).
- Alternative Flow: If the request is retried with the **same idempotency key** as a previously successful checkout by the same customer, the system must **not** create a second order; it must return the result of the original order creation (HTTP 200, not 201), unchanged.
- Exception Flow:
  - Cart is empty → `VALIDATION_ERROR` (400).
  - One or more SKUs cannot be fully reserved at confirmation time → `BUSINESS_RULE_VIOLATION` (422, rule BR-004), identifying which SKU(s) are insufficient; no order is created; any reservations already made for other SKUs in the same request must be rolled back/released (all-or-nothing reservation for a single checkout).
  - Same idempotency key resubmitted with a **different** payload than the original request → `BUSINESS_RULE_VIOLATION` (422, conflicting idempotent request); the system must not silently accept the new payload as if it were the same operation.
- Postconditions: Order exists in `RESERVED` state; inventory reservations exist for every line; order breakdown and snapshots are persisted and immutable from this point forward.
- Business Rules: BR-004, BR-009, BR-010, BR-013, BR-017.
- Acceptance Criteria:
```gherkin
Feature: Checkout
Scenario: Successful checkout
  Given the cart is valid and every SKU has sufficient available stock
  And the customer is authenticated and has selected a shipping address
  When the customer confirms checkout with a new idempotency key
  Then an order is created in RESERVED status
  And inventory is reserved for every line
  And the order contains a snapshot of address, items, and monetary breakdown

Scenario: Empty cart
  Given the cart has no items
  When the customer attempts checkout
  Then the system rejects the request with VALIDATION_ERROR

Scenario: Insufficient stock at confirmation
  Given one SKU in the cart no longer has sufficient available stock
  When the customer confirms checkout
  Then the system rejects with BUSINESS_RULE_VIOLATION identifying the insufficient SKU
  And no order is created

Scenario: Duplicate checkout request (idempotency)
  Given a checkout request was already successfully processed with idempotency key K
  When the same request is resubmitted with idempotency key K
  Then no second order is created
  And the result of the original order is returned
```
- Traceability: US-009 | Entity: orders, order_item, inventory_reservation | API: API-ORD-001 | Events: OrderCreated | BR: BR-004, BR-009, BR-010, BR-013, BR-017 | NFR: NFR-IDEMPOTENCY-001, NFR-LOCK-001

---

**FR-010 — Process Payment Result (full lifecycle — see Section 9 for complete Payment Flow)**
- Description: Handles the outcome of a payment attempt for an order and drives the corresponding order transition. Full multi-step lifecycle (initiation → processing → callback → reconciliation) is specified in Section 9; this FR governs the callback-processing step specifically.
- Actor: ACT-002 (initiates payment indirectly via checkout-to-payment flow) / ACT-005 (delivers the result)
- Preconditions: A `payment_transaction` exists in state `INITIATED` for the order; the order is in state `RESERVED`.
- Trigger: Payment Gateway sends a result callback.
- Main Flow (SUCCESS): Validate callback authenticity (mechanism `TECH_STACK_DECISION_REQUIRED`) → mark `payment_transaction` as `SUCCEEDED` → transition order `RESERVED → PAID` → record timeline entry → publish `PaymentSucceeded` → trigger fulfillment initiation.
- Alternative Flow (FAILURE): Mark `payment_transaction` as `FAILED` → transition order `RESERVED → PAYMENT_FAILED` → release the associated inventory reservation (FR-020) → record timeline entry → publish `PaymentFailed`.
- Exception Flow:
  - No callback received within the configured timeout window → treat as timeout: mark `payment_transaction` as `TIMEOUT`, transition order `RESERVED → EXPIRED`, release reservation, record timeline entry (see Section 9.4).
  - A second SUCCESS callback arrives for an order already `PAID` → reject processing it a second time (do not create a second successful `payment_transaction` for that order — BR-002); record an `exception_record` of type `DUPLICATE_PAYMENT` for administrative visibility. This is a `BUSINESS_RULE_VIOLATION` (422) from the callback endpoint's perspective.
  - A callback arrives referencing an order that is not in `RESERVED` state (late callback) → do not apply the payment result if the order has already moved past the point where the result is applicable (e.g., already `EXPIRED` or `CANCELLED`); record an `exception_record` of type `LATE_OR_STALE_PAYMENT_CALLBACK` for administrative review rather than silently discarding it.
- Postconditions: Order in `PAID`, `PAYMENT_FAILED`, or `EXPIRED`; `payment_transaction` finalized; reservation consumed or released accordingly.
- Business Rules: BR-002, BR-003.
- Acceptance Criteria: success, failure, timeout, duplicate-callback, late-callback scenarios (see Section 9 for full Gherkin).
- Traceability: US-010 | Entity: payment_transaction, orders | API: API-PAY-001 | Events: PaymentSucceeded, PaymentFailed, PaymentTimeout | BR: BR-002, BR-003 | NFR: NFR-IDEMPOTENCY-001, NFR-RETRY-001

---

**FR-011 — Track Order (View Status & Timeline)**
- Description: Shows current status and the full Order Timeline for an order.
- Actor: ACT-002
- Preconditions: Order belongs to the requesting customer.
- Trigger: Open order tracking page.
- Main Flow: Return current status + every `order_timeline_event` for the order, ordered chronologically.
- Alternative Flow: None.
- Exception Flow: Order belongs to a different customer → `AUTHORIZATION_FAILED` (403).
- Postconditions: Read-only.
- Business Rules: BR-008.
- Traceability: US-011 | Entity: orders, order_timeline_event | API: API-ORD-002 | BR: BR-008

---

**FR-012 — Cancel Order (Customer-initiated)**
- Description: Allows the customer to cancel their own order while it remains within the allowed cancellation window.
- Actor: ACT-002
- Preconditions: Order is in a state that permits customer cancellation (see Order State Machine, Section 6 — currently: `RESERVED` or `PAID`, and not yet `PACKING` or beyond; see Decision Register CQ-002 for the exact cutoff confirmation).
- Trigger: Cancel request.
- Main Flow: Verify allowed state → transition order to `CANCELLED` → release associated inventory reservation (FR-020) → record timeline entry.
- Alternative Flow: None.
- Exception Flow: Order has passed the allowed cutoff (e.g., already `PACKING` or beyond) → `INVALID_STATE` (409); order already `CANCELLED` → `BUSINESS_RULE_VIOLATION` (422, BR-001).
- Postconditions: Order `CANCELLED`; reservation released.
- Business Rules: BR-001, BR-006, BR-007.
- Traceability: US-012 | Entity: orders, inventory_reservation | API: API-ORD-003 | BR: BR-001, BR-006, BR-007

---

**FR-013 — Send Order Status Notification**
- Description: Sends a notification to the customer whenever the order's status changes, using the template configured for that event.
- Actor: ACT-004 / ACT-006
- Preconditions: A `notification_template` is configured for the triggering event code.
- Trigger: `OrderStatusChanged` event.
- Main Flow: Select the template for the event → dispatch to the configured channel → record `notification_log` (`status = SENT`).
- Alternative Flow: None.
- Exception Flow: Channel unavailable/dispatch fails → record `notification_log` (`status = FAILED`); **this must never block order processing** — the order state transition and its side effects proceed regardless of notification outcome; the system retries dispatch per the Retry requirements (Section 14).
- Postconditions: `notification_log` entry created; order processing unaffected by notification outcome.
- Business Rules: None directly; operational constraint: notification failure must never block the order pipeline.
- Traceability: US-013 | Entity: notification_log, notification_template | API: API-NOTI-002 | Events: consumes OrderStatusChanged

---

**FR-014 — Manage Products (CRUD)**
- Description: Catalog Admin creates, edits, deletes products.
- Actor: ROLE-002
- Preconditions: Caller holds permission `PRODUCT_WRITE`.
- Trigger: Product create/edit/delete action.
- Main Flow: Apply the change to `product`.
- Alternative Flow: None.
- Exception Flow: Caller lacks permission → `AUTHORIZATION_FAILED` (403); hard-delete attempted on a product referenced by an incomplete order → `BUSINESS_RULE_VIOLATION` (422, BR-005) — the system must instead offer/apply a "discontinue" (deactivate) transition rather than physical deletion.
- Postconditions: Catalog updated or change rejected.
- Business Rules: BR-005.
- Traceability: US-014 | Entity: product | API: API-CAT-004 | BR: BR-005 | NFR: NFR-SEC-002

---

**FR-015 — Manage Categories (CRUD)**
- Description: Catalog Admin creates/edits categories, at most two levels deep (parent/child; a child category cannot itself have children) per Decision Register CQ-015 default.
- Actor: ROLE-002
- Preconditions: Caller holds permission `PRODUCT_WRITE`.
- Trigger: Category create/edit action.
- Main Flow: Create/update category with a unique name.
- Alternative Flow: None.
- Exception Flow: Duplicate name → `VALIDATION_ERROR` (400); attempt to nest beyond two levels → `VALIDATION_ERROR` (400).
- Postconditions: Category tree updated.
- Business Rules: BR-005.
- Traceability: US-015 | Entity: category | API: API-CAT-005 | BR: BR-005 | NFR: NFR-SEC-002

---

**FR-016 — Adjust Inventory (Basic)**
- Description: Inventory Admin adjusts the physical on-hand quantity for a SKU.
- Actor: ROLE-003
- Preconditions: Caller holds permission `INVENTORY_WRITE`; SKU exists.
- Trigger: Inventory adjustment action.
- Main Flow: Update `quantity_on_hand` → recompute `quantity_available` (= on_hand − reserved).
- Alternative Flow: None.
- Exception Flow: Resulting on-hand quantity would be negative → `BUSINESS_RULE_VIOLATION` (422).
- Postconditions: Inventory figures updated.
- Business Rules: BR-004, BR-005, BR-014.
- Traceability: US-016 | Entity: inventory | API: API-INV-001 | BR: BR-004, BR-005, BR-014 | NFR: NFR-SEC-002

---

**FR-017 — Manage Orders (Admin), enforced through the state machine**
- Description: Order Operations Admin views orders and transitions their status; every transition must go through the formally defined state machine (Section 6) — direct field overwrite of order status is never permitted, for any role including Super Admin.
- Actor: ROLE-004
- Preconditions: Caller holds `ORDER_READ` (for viewing) and `ORDER_STATE_TRANSITION` (for transitioning).
- Trigger: Admin views orders / requests a transition.
- Main Flow: List/inspect orders → request a transition to a target state that is valid per Section 6 from the order's current state → apply transition → record timeline entry → record audit log entry.
- Alternative Flow: None.
- Exception Flow: Requested transition is not allowed from the current state (e.g., skipping an intermediate state, or transitioning out of `CANCELLED`) → `BUSINESS_RULE_VIOLATION` (422, referencing BR-001/BR-007/BR-011); caller lacks permission → `AUTHORIZATION_FAILED` (403).
- Postconditions: Order status updated only via valid transition; `order_timeline_event` and `audit_log` entries created.
- Business Rules: BR-001, BR-007, BR-008, BR-011.
- Traceability: US-017, US-032 | Entity: orders, order_timeline_event | API: API-ORD-004, API-ORD-005 | BR: BR-001, BR-007, BR-008, BR-011 | NFR: NFR-AUDIT-001, NFR-RBAC-001

---

**FR-018 — Manage Users (Admin)**
- Description: Super Admin views and locks/unlocks back-office and customer accounts.
- Actor: ROLE-001
- Preconditions: Caller holds `USER_ACCOUNT_MANAGE`.
- Trigger: Lock/unlock action.
- Main Flow: Update `user_account.status`.
- Alternative Flow: None.
- Exception Flow: Caller lacks permission → `AUTHORIZATION_FAILED` (403).
- Postconditions: Account status updated; a locked account cannot authenticate (FR-002) until unlocked.
- Business Rules: BR-005, BR-018.
- Traceability: US-018 | Entity: user_account, role, user_role | API: API-IAM-004, API-IAM-005 | BR: BR-005, BR-018 | NFR: NFR-SEC-002

---

**FR-019 — Inventory Reservation**

- Description: The system must atomically reserve inventory for all SKUs in an order during order creation, preventing overselling.
- Actor: ACT-004
- Preconditions: Sufficient available inventory exists for all SKUs and quantities in the order.
- Trigger: Order Service requests inventory reservation during order creation (FR-009).
 Main Flow:

  1. Order Service identifies the SKUs and quantities that must be reserved.
  2. Inventory Service atomically reserves inventory for all required SKUs.
  3. If successful, the system creates an `inventory_reservation` with `status = ACTIVE`.
  4. Inventory Service publishes `InventoryReserved`.
  5. The Order is allowed to transition to `RESERVED`.
- Alternative Flow: None.
- Exception Flow: If inventory is insufficient or concurrent requests compete for the remaining available inventory, the entire reservation attempt fails; no partial reservation is allowed; the Order must not transition to `RESERVED`; the system publishes `InventoryReservationFailed`.
- Postconditions: Available inventory is reduced by the reserved quantities, and the corresponding reservation is in `ACTIVE` state.
- Business Rules: BR-004, BR-014.
- Acceptance Criteria: US-019.
- Traceability: US-019 | Entity: inventory, inventory_reservation | Event: InventoryReserved, InventoryReservationFailed | BR: BR-004,014 | NFR: NFR-LOCK-001


---

**FR-020 — Inventory Compensation**

- Description: The system must automatically release reserved inventory when payment fails, the reservation expires, or the order is cancelled before the reservation is consumed.
- Actor: ACT-004
- Preconditions: The corresponding `inventory_reservation` is in `ACTIVE` state.
- Trigger: Payment failure, reservation expiration, or order cancellation before the Order reaches `PAID`.
- Main Flow: Transition the reservation from `ACTIVE` to `RELEASED` or `EXPIRED`, depending on the cause → restore the reserved quantity to available inventory.
- Alternative Flow: None.
- Exception Flow: If the reservation is already `CONSUMED`, FR-020 must not release it. Any subsequent cancellation, refund, or inventory compensation belongs to a separate business flow.
- Postconditions: The reservation is in `RELEASED` or `EXPIRED` state, and available inventory is restored accurately.
- Business Rules: BR-003.
- Acceptance Criteria: US-020.
- Traceability: US-020 | Entity: inventory_reservation, inventory | BR: BR-003 | NFR: NFR-RESILIENCE-001, NFR-RETRY-001


---

**FR-021 — Monitor Fulfillment**
- Description: Order Operations Admin views the fulfillment/shipping pipeline status of an order.
- Actor: ROLE-004
- Preconditions: Order has been paid.
- Trigger: Open fulfillment monitoring screen.
- Main Flow: Show completed steps and current step of the shipment pipeline.
- Alternative Flow: An order shows no status update for an abnormally long period → it is flagged/highlighted as needing attention (feeds FR-042).
- Exception Flow: None additional.
- Postconditions: Read-only (except escalation handled by FR-042).
- Business Rules: BR-007.
- Traceability: US-021 | Entity: shipment, orders | API: API-FUL-002 | BR: BR-007 | NFR: NFR-OBS-001

---

**FR-022 — Manage Customer Address Book**
- Description: Customer adds/edits/deletes addresses, selects a default, and selects one at checkout.
- Actor: ACT-002
- Preconditions: Authenticated.
- Trigger: Address book action, or address selection at checkout.
- Main Flow: Create/edit/delete `customer_address`; at checkout, the selected address is copied (snapshotted) into the order.
- Alternative Flow: None.
- Exception Flow: Invalid address data → `VALIDATION_ERROR` (400); accessing an address not owned by the caller → `AUTHORIZATION_FAILED` (403).
- Postconditions: `customer_address` updated; **editing or deleting an address afterward never changes any already-placed order** (BR-017).
- Business Rules: BR-017.
- Traceability: US-023 | Entity: customer_address | API: API-IAM-003 | BR: BR-017

---

**FR-023 — Manage SKU/Variant**
- Description: Catalog Admin creates/edits SKUs (variants) of a product.
- Actor: ROLE-002
- Preconditions: Parent product exists.
- Trigger: SKU create/edit action.
- Main Flow: Create a SKU with a unique SKU code and a combination of attribute values → an `inventory` row is initialized for the new SKU.
- Alternative Flow: A product that declares no variant automatically receives one default SKU (ASM-007 — pending confirmation per CQ-011).
- Exception Flow: The requested attribute-value combination already exists for this product → `BUSINESS_RULE_VIOLATION` (422).
- Postconditions: New SKU available for pricing/inventory.
- Business Rules: BR-014.
- Traceability: US-026 | Entity: sku, sku_attribute_value | API: API-CAT-006 | BR: BR-014

---

**FR-024 — Manage Attributes**
- Description: Catalog Admin defines attribute types (e.g., Color, Size) and their possible values, used to compose SKUs.
- Actor: ROLE-002
- Preconditions: None.
- Trigger: Attribute/value create or edit.
- Main Flow: Create an attribute type; create values under it.
- Alternative Flow: None.
- Exception Flow: Duplicate attribute name, or duplicate value under the same attribute → `VALIDATION_ERROR` (400).
- Postconditions: Attribute/value available for SKU composition.
- Business Rules: BR-014.
- Traceability: US-026 | Entity: product_attribute, product_attribute_value | API: API-CAT-007 | BR: BR-014

---

**FR-025 — Manage Product/SKU Media**
- Description: Catalog Admin uploads/removes images associated with a product or a specific SKU.
- Actor: ROLE-002
- Preconditions: Product/SKU exists.
- Trigger: Upload/remove media action.
- Main Flow: Create/remove a `product_media` record linked to the product (and optionally a specific SKU).
- Alternative Flow: None.
- Exception Flow: Invalid file/format → `VALIDATION_ERROR` (400).
- Postconditions: Media reflected on the product detail page.
- Business Rules: None directly.
- Traceability: US-026 | Entity: product_media | API: API-CAT-008

---

**FR-026 — Manage Pricing**
- Description: Catalog Admin sets a base price and, optionally, a time-limited promotional price for a SKU; price history is retained.
- Actor: ROLE-002
- Preconditions: SKU exists.
- Trigger: Price/promotion create or update.
- Main Flow: Creating a new base price closes (sets `effective_to` on) the currently active base-price record and opens a new one. Creating a promotional price creates a `price_promotion` row with a `start_at`/`end_at` window.

  **Effective price rule (used by FR-005, FR-009, FR-027, FR-028):** at any given instant, a SKU's effective price is its active `price_promotion.sale_price` if the current instant falls within a promotion's `start_at`–`end_at` window and the promotion is active; otherwise it is the currently active `price.base_price` (the price row whose `effective_to` is null or in the future and whose `effective_from` has already passed).
- Alternative Flow: None.
- Exception Flow: `start_at ≥ end_at` for a promotion → `VALIDATION_ERROR` (400).
- Postconditions: New price/promotion takes effect for future transactions; **already-placed orders are never retroactively affected** (BR-013), because their price was already snapshotted at order-creation time (FR-028).
- Business Rules: BR-013.
- Traceability: US-030 | Entity: price, price_promotion | API: API-PRC-001, API-PRC-002 | BR: BR-013

---

**FR-027 — Compute Order Monetary Breakdown**
- Description: At checkout, computes Subtotal, Shipping Fee, Discount, and Grand Total for the order.
- Actor: ACT-004
- Preconditions: Checkout confirmed (FR-009).
- Trigger: Checkout confirmation, as an internal step of FR-009.
- Main Flow: `subtotal_amount` = sum over order lines of (effective unit price × quantity); `shipping_fee_amount` = a value determined by business logic not yet specified beyond "must exist as a field" (see Decision Register — no calculation rule for shipping fee has been confirmed by Business Analysis; if no rule is confirmed, this must default in a way that is explicitly documented in implementation, not silently invented); `discount_amount` = 0 in the MVP (ASM-008, Coupon is Phase-2 and not implemented); `grand_total_amount` = `subtotal_amount` + `shipping_fee_amount` − `discount_amount`.
- Alternative Flow: None.
- Exception Flow: Effective price cannot be determined for a SKU at breakdown time → `DEPENDENCY_ERROR` or `INTERNAL_ERROR` depending on cause; checkout must not silently proceed with a guessed price.
- Postconditions: `orders` row stores the full breakdown.
- Business Rules: BR-013.
- Traceability: US-009 | Entity: orders | BR: BR-013

---

**FR-028 — Snapshot Order Item**
- Description: For every order line, permanently records a copy (snapshot) of the product name, SKU code, attribute description, and unit price as they existed at the moment of order creation.
- Actor: ACT-004
- Preconditions: Order is being created; effective price has been determined.
- Trigger: Order creation (FR-009, step 4).
- Main Flow: Copy `product_name_snapshot`, `sku_code_snapshot`, `attribute_snapshot`, `unit_price_snapshot` into `order_item`; also compute and store `line_total = unit_price_snapshot × quantity`.
- Alternative Flow: None.
- Exception Flow: None (internal step of FR-009).
- Postconditions: `order_item` is immutable thereafter — subsequent changes to the source `product`/`sku`/`price` records must never alter an already-created `order_item` (this protects against Edge Case EC-014: a product later deleted/discontinued must not corrupt historical order display).
- Business Rules: BR-013.
- Traceability: US-009 | Entity: order_item | BR: BR-013

---

**FR-029 — Record & Display Order Timeline**
- Description: Every order status transition, from any actor (customer, admin, or system), must produce exactly one immutable `order_timeline_event` record, viewable by the owning customer and by Order Operations Admin.
- Actor: ACT-004 / ROLE-004
- Preconditions: Order exists.
- Trigger: Any order status transition.
- Main Flow: Insert a record containing `from_status`, `to_status`, `actor_id` (nullable for pure-system transitions), `actor_type` (`CUSTOMER`/`BACK_OFFICE`/`SYSTEM`), an optional `note`, and `occurred_at`.
- Alternative Flow: None.
- Exception Flow: None — there is intentionally **no API operation that updates or deletes a timeline record** once created.
- Postconditions: Complete, append-only, immutable transition history exists for every order.
- Business Rules: BR-008.
- Traceability: US-011, US-032 | Entity: order_timeline_event | API: API-ORD-002, API-ORD-005 | BR: BR-008 | NFR: NFR-AUDIT-001

---

**FR-030 — Adjust Inventory with Reason Code**
- Description: Inventory Admin adjusts stock with a mandatory reason code, generating an immutable audit trail entry.
- Actor: ROLE-003
- Preconditions: SKU exists.
- Trigger: Inventory adjustment with reason.
- Main Flow: Update `inventory.quantity_on_hand` → insert an `inventory_adjustment_log` row capturing before-quantity, after-quantity, delta, reason code, actor, and timestamp.
- Alternative Flow: None.
- Exception Flow: Resulting quantity would be negative → `BUSINESS_RULE_VIOLATION` (422).
- Postconditions: Inventory updated; an immutable audit entry exists — **there is no API operation to modify or delete this entry, ever, including for Super Admin.**
- Business Rules: BR-015.
- Traceability: US-031 | Entity: inventory, inventory_adjustment_log | API: API-INV-001 | BR: BR-015 | NFR: NFR-AUDIT-002

---

**FR-031 — Inventory Audit Trail**
- Description: Inventory Admin queries the history of stock adjustments for a SKU.
- Actor: ROLE-003
- Preconditions: None.
- Trigger: Audit trail query.
- Main Flow: Return matching `inventory_adjustment_log` rows for the given SKU/time range.
- Alternative Flow: None.
- Exception Flow: An attempt (by any actor, including Super Admin) to modify or delete a returned record must be rejected — no such API exists.
- Postconditions: Read-only.
- Business Rules: BR-015.
- Traceability: US-031 | Entity: inventory_adjustment_log | API: API-INV-002 | BR: BR-015 | NFR: NFR-AUDIT-002

---

**FR-032 — Admin Dashboard / Business Overview**
- Description: Displays aggregated operational figures: today's orders by status, failed payments, stuck orders, low-stock alerts.
- Actor: ROLE-001 / ROLE-004
- Preconditions: Caller has dashboard-view access.
- Trigger: Open dashboard.
- Main Flow: Aggregate figures from Order/Inventory/Payment/Exception data → display.
- Alternative Flow: None.
- Exception Flow: Caller lacks permission → `AUTHORIZATION_FAILED` (403).
- Postconditions: Read-only.
- Business Rules: None directly.
- Traceability: Entity: orders, inventory, payment_transaction, exception_record (aggregate) | API: API-DASH-001

---

**FR-033 — Role & Permission Management (RBAC administration)**
- Description: Super Admin creates roles, assigns/revokes permissions to roles, and assigns/revokes roles on back-office user accounts.
- Actor: ROLE-001
- Preconditions: Caller holds `USER_ROLE_MANAGE`.
- Trigger: Role/permission administration action.
- Main Flow: Assign role to account → account gains exactly that role's permission set.
- Alternative Flow: A newly created role starts with **zero** permissions (deny-by-default, BR-018); permissions must be explicitly granted afterward.
- Exception Flow: Invalid/non-existent role code → `BUSINESS_RULE_VIOLATION` (422); caller lacks permission → `AUTHORIZATION_FAILED` (403).
- Postconditions: `user_role`/`role_permission` updated; an `audit_log` entry (`action_type = ROLE_ASSIGN`) is created.
- Business Rules: BR-018.
- Traceability: US-034 | Entity: role, permission, role_permission, user_role | API: API-RBAC-001 | BR: BR-018 | NFR: NFR-RBAC-001

---

**FR-034 — Audit Log Query**
- Description: Super Admin searches the aggregated sensitive-action audit log by actor, time range, and action type.
- Actor: ROLE-001
- Preconditions: Caller holds `AUDIT_READ`.
- Trigger: Audit log search.
- Main Flow: Return matching `audit_log` rows.
- Alternative Flow: None.
- Exception Flow: Caller lacks permission → `AUTHORIZATION_FAILED` (403).
- Postconditions: Read-only. **No update/delete operation on `audit_log` exists in this system, for any actor.**
- Business Rules: BR-015.
- Traceability: US-036 | Entity: audit_log | API: API-AUDIT-001 | BR: BR-015 | NFR: NFR-AUDIT-002

---

**FR-035 — Notification Template Management**
- Description: Maintains the content template used for each notification-triggering event.
- Actor: ROLE-001 / ROLE-002
- Preconditions: None.
- Trigger: Template create/edit.
- Main Flow: Update `notification_template` content for an event code.
- Alternative Flow: None.
- Exception Flow: Caller lacks permission → `AUTHORIZATION_FAILED` (403).
- Postconditions: Notifications sent **after** the change use the new content; notifications already sent are unaffected.
- Business Rules: None directly.
- Traceability: US-037 | Entity: notification_template | API: API-NOTI-001

---

**FR-036 — Notification Delivery Log & Manual Retry**
- Description: Order Operations Admin views notification delivery outcomes and can manually trigger a resend.
- Actor: ROLE-004
- Preconditions: A `notification_log` record exists.
- Trigger: View log / trigger resend.
- Main Flow: Display log entries; on manual retry, resend and update `attempt_count`/`last_attempt_at`.
- Alternative Flow: None.
- Exception Flow: Channel still unavailable → record another failed attempt, does not block anything else.
- Postconditions: `notification_log` updated.
- Business Rules: None directly.
- Traceability: Entity: notification_log | API: API-NOTI-002

---

**FR-037 — Business Configuration Management**
- Description: Super Admin views/updates business parameters (e.g., reservation TTL, cancellation cutoff status) with time-versioned, non-retroactive effect.
- Actor: ROLE-001
- Preconditions: Caller holds `CONFIG_WRITE`.
- Trigger: Configuration update.
- Main Flow: Close the currently active configuration record's `effective_to`, create a new record with a new `effective_from`, effective going forward only.
- Alternative Flow: None.
- Exception Flow: Caller lacks permission → `AUTHORIZATION_FAILED` (403).
- Postconditions: Transactions created **after** the change use the new value; transactions already in flight retain the value that was effective when they began (BR-019); an `audit_log` entry is mandatory for every configuration change.
- Business Rules: BR-019.
- Traceability: US-038 | Entity: business_configuration | API: API-CFG-001 | BR: BR-019 | NFR: NFR-CONFIG-001

---

**FR-038 — Exception / Failed Transaction Board**
- Description: Centralizes business exceptions (payment failures, triggered compensations, stuck orders, notification failures, duplicate/late payment callbacks) for administrative review and resolution tracking.
- Actor: ROLE-004 / ROLE-001
- Preconditions: None.
- Trigger: An exception-generating event occurs elsewhere in the system, or an admin opens the board.
- Main Flow: Display `exception_record` rows in `OPEN` status; admin marks a record `RESOLVED` with a note.
- Alternative Flow: None.
- Exception Flow: Caller lacks permission → `AUTHORIZATION_FAILED` (403).
- Postconditions: `exception_record.status` updated; an `audit_log` entry records the resolution action.
- Business Rules: None directly — this is a read/administration model built atop events already required elsewhere.
- Traceability: US-039 | Entity: exception_record | API: API-EXC-001

---

**FR-039 — Payment Reconciliation View**
- Description: Order Operations Admin views payment transaction status/history, sees anomalous transactions, and may trigger a controlled manual reconciliation.
- Actor: ROLE-004
- Preconditions: Caller holds `PAYMENT_RECONCILE_VIEW`.
- Trigger: Open reconciliation view / submit a manual reconciliation.
- Main Flow: List anomalous transactions (timeout/failed/mismatched). For manual reconciliation, the admin must supply an `evidence_reference` and a mandatory `reason`; the system marks the transaction as reconciled and, if appropriate, drives the order transition.
- Alternative Flow: None.
- Exception Flow: An attempt to mark an order `PAID` **without** going through the evidence+reason-based reconciliation process → `BUSINESS_RULE_VIOLATION` (422, BR-012) — **there is no "set status directly" capability for payment, for any role.**
- Postconditions: `payment_transaction`/order updated only through the controlled path; a mandatory `audit_log` entry is created, including the supplied reason.
- Business Rules: BR-012.
- Traceability: US-033 | Entity: payment_transaction | API: API-PAY-002, API-PAY-003 | BR: BR-012

---

**FR-040 — Fulfillment/Shipping Management (Admin)**
- Description: Order Operations Admin records simulated shipping information (carrier, tracking code) and drives the Shipment lifecycle, in lock-step with the Order lifecycle.
- Actor: ROLE-004
- Preconditions: Order is in a state that allows shipment progression (order must be at least `PACKING` before a tracking code may be attached — see Section 6/7 for the exact coupling).
- Trigger: Update shipment info / advance shipment status.
- Main Flow: Attach carrier/tracking data; advance `shipment.status` per the Shipment State Machine (Section 7).
- Alternative Flow: None.
- Exception Flow: An attempt to attach tracking data or advance shipment status while the order has not yet reached `PACKING` → `BUSINESS_RULE_VIOLATION` (422, BR-011).
- Postconditions: `shipment` updated; a corresponding `order_timeline_event` is recorded.
- Business Rules: BR-011.
- Traceability: US-040 | Entity: shipment | API: API-FUL-001 | BR: BR-011

---

**FR-041 — Customer Support Assisted Actions**
- Description: Customer Support performs a strictly whitelisted set of actions to help a customer, without the ability to alter monetary values or inventory.
- Actor: ROLE-005
- Preconditions: The requested `action_type` is one of: `RESEND_NOTIFICATION`, `UNLOCK_ACCOUNT`, `INITIATE_CANCEL` (subject to the same cancellation-window rule as FR-012/BR-006).
- Trigger: Customer contacts support requesting help.
- Main Flow: Perform the whitelisted action → record `audit_log` and, where relevant, `notification_log`.
- Alternative Flow: None.
- Exception Flow: Any attempt to perform an action outside the whitelist (e.g., modifying an order's Grand Total, adjusting inventory) → `AUTHORIZATION_FAILED` (403, BR-016) — **no API exists for such actions under this role; this is not an application-level check on an otherwise-existing capability, the capability itself does not exist for this role.**
- Postconditions: Whitelisted action completed; full audit trail exists.
- Business Rules: BR-016.
- Traceability: US-035, US-041 | Entity: user_account, notification_log, orders (limited) | API: API-SUP-001 | BR: BR-016 | NFR: NFR-RBAC-001

---

**FR-042 — Stuck Order Detection & Escalation**
- Description: Detects orders with no status update for an abnormally long time and lets Order Operations Admin re-trigger processing without forcing a status.
- Actor: ROLE-004
- Preconditions: A configured "stuck" time threshold has been exceeded (specific value: `NEEDS_CLARIFICATION`, see Section 17/Decision Register).
- Trigger: Scheduled detection job, or admin opens the stuck-order list.
- Main Flow: Flag the order as stuck → admin selects "retry/resume" → the system **re-publishes the event(s)** needed for the pipeline to continue (e.g., re-attempting a downstream call that previously failed) rather than directly setting a new status.
- Alternative Flow: None.
- Exception Flow: There is no capability to force-set a status as part of this flow — doing so would violate BR-011; if retry does not resolve the stuck condition, the order remains stuck and flagged.
- Postconditions: An event is re-emitted; the order progresses only if the underlying blocking condition has actually been resolved.
- Business Rules: BR-011.
- Traceability: US-021, US-042 | Entity: orders | API: API-ORD-006, API-ORD-007 | BR: BR-011

---

## 5. Business Rules (authoritative statements)

| ID | Rule |
|---|---|
| BR-001 | An order can never transition from `CANCELLED` to any other state. |
| BR-002 | An order can have at most one `SUCCEEDED` payment transaction; a second success callback for an already-`PAID` order must be rejected and recorded as an exception, never processed as a second payment. |
| BR-003 | Inventory reserved for an order must be released if payment does not complete within the allowed reservation window. (Exact window duration: `NEEDS_CLARIFICATION`, see Decision Register CQ-003/CQ-013 — must be sourced from Business Configuration, not hardcoded.) |
| BR-004 | A SKU may not be added to a cart, nor included in a successful checkout, if the requested quantity exceeds the SKU's currently available stock (on-hand minus currently reserved). |
| BR-005 | Only a role holding the applicable permission may create/edit/delete product, category, or SKU data, or change another user's account status/role. Hard deletion of a product/category referenced by an incomplete order is disallowed; deactivation must be used instead. |
| BR-006 | A customer may self-cancel an order only while it remains in a state that precedes the fulfillment cutoff point. (Exact cutoff: `NEEDS_CLARIFICATION`, see Decision Register CQ-002 — current default used throughout this document is "before `PACKING`".) |
| BR-007 | An order must follow a defined, finite sequence of states; `CANCELLED`/failure branches are permitted only from the specific states enumerated in Section 6 — never from arbitrary states. |
| BR-008 | Every order status change must be recorded with the identity of the acting party (or `SYSTEM` if automated) and the timestamp of the change. |
| BR-009 | Checkout requires an authenticated session (guest checkout is not permitted unless CQ-001 is confirmed otherwise). |
| BR-010 | Multiple submissions of the same checkout request (same idempotency key) must never result in more than one order being created. |
| BR-011 | No back-office role — **including Super Admin** — may set an order's, a shipment's, or a payment's status directly, bypassing the formally defined state machine (Sections 6, 7, 9). Every status change must go through a validated transition. |
| BR-012 | An order may never be marked `PAID` manually unless the change goes through the controlled reconciliation procedure (FR-039), which mandatorily requires an evidence reference and a logged reason. |
| BR-013 | The price and the shipping address applied to an order must be captured (snapshotted) at the moment the order is created; subsequent changes to catalog price or to the customer's saved address must never retroactively alter an existing order. |
| BR-014 | Inventory is tracked at the SKU (variant) level, never at the parent Product level. |
| BR-015 | Every administrative action that changes a materially significant business record (order, inventory, pricing, role/permission, business configuration) must produce an immutable Audit Log entry. |
| BR-016 | The Customer Support role may never alter an order's monetary values, nor directly adjust inventory; it may only perform the specific whitelisted actions defined in FR-041. |
| BR-017 | The shipping address applied to an order must be a snapshot captured at order-creation time; later changes to the customer's saved address book must never affect a previously placed order. |
| BR-018 | A newly created role has no permissions by default (deny-by-default); permissions must be explicitly granted. |
| BR-019 | A change to Business Configuration may only be performed by Super Admin, must be logged in the Audit Log, and takes effect only for transactions initiated after the change (never retroactively). |

---

## 6. Order State Machine

**Order States:** `RESERVED`, `PAID`, `PACKING`, `SHIPPED`, `COMPLETED`, `PAYMENT_FAILED`, `EXPIRED`, `CANCELLED`.

**Inventory Reservation Transition Rule**
The Order and Inventory Reservation state machines are coordinated but distinct. When the Order successfully transitions from `RESERVED` to `PAID` as a result of a successful payment, the associated `inventory_reservation` must transition from `ACTIVE` to `CONSUMED`. The `PACKING` state does not control or trigger consumption of the inventory reservation.
Therefore: `PaymentSucceeded → Order: RESERVED → PAID → Inventory Reservation: ACTIVE → CONSUMED` Payment failure, order cancellation before `PAID`, and reservation expiration release the `ACTIVE` reservation through the compensation rules defined in FR-020.


**Relationship to Shipment:** The Order state and the Shipment state (Section 7) are two coordinated but distinct state machines. `PACKING` and `SHIPPED` on the Order are driven by, and must stay consistent with, the corresponding Shipment states `PACKING` and `SHIPPED`. The Order's `COMPLETED` state corresponds to the Shipment reaching `DELIVERED`. There is **no ambiguity** to be left open here: the confirmed relationship is `Order.SHIPPED` occurs when `Shipment.SHIPPED` occurs, and `Order.COMPLETED` occurs only when `Shipment.DELIVERED` occurs (i.e., the sequence is Order `SHIPPED → COMPLETED`, gated by the Shipment reaching `DELIVERED` — the Order state machine does not expose a separate "DELIVERED" state of its own; delivery confirmation is what causes the `SHIPPED → COMPLETED` order transition to fire).

| Transition ID | From State | To State | Trigger | Actor/System | Preconditions | Guards | Side Effects | Event |
|---|---|---|---|---|---|---|---|---|
| ORD-T01 | `[none]` | `RESERVED` | Checkout confirmed | ACT-002 (via ACT-004 orchestration) | Cart non-empty; address selected; stock available for every line | All-or-nothing reservation must succeed for every line | Create order; create `inventory_reservation` (ACTIVE) per line; snapshot address & items; compute breakdown; write timeline entry (`null → RESERVED`) | `OrderCreated` |
| ORD-T02 | `RESERVED` | `PAID` | Payment callback SUCCESS | ACT-005 (processed by ACT-004) | Exactly one payment transaction reaches `SUCCEEDED` for this order (BR-002) | Order must currently be `RESERVED`; a second success for an already-`PAID` order is rejected, not applied | Mark `payment_transaction` `SUCCEEDED`; write timeline entry; trigger fulfillment initiation | `PaymentSucceeded` |
| ORD-T03 | `RESERVED` | `PAYMENT_FAILED` | Payment callback FAILED | ACT-005 (processed by ACT-004) | Order must currently be `RESERVED` | — | Mark `payment_transaction` `FAILED`; release reservation (FR-020); write timeline entry | `PaymentFailed` |
| ORD-T04 | `RESERVED` | `EXPIRED` | Reservation/payment TTL exceeded with no callback received | ACT-004 (scheduled process) | Order still `RESERVED` when TTL check runs | — | Mark `payment_transaction` `TIMEOUT` (if one exists); release reservation; write timeline entry | `PaymentTimeout` |
| ORD-T05 | `RESERVED` | `CANCELLED` | Customer (or Customer Support on the customer's behalf) requests cancellation | ACT-002 / ROLE-005 (whitelisted) | Order still before the cutoff defined by BR-006 | Must satisfy BR-006 cutoff rule | Release reservation; write timeline entry | `OrderCancelled` |
| ORD-T06 | `PAID` | `CANCELLED` | Customer (or Customer Support) requests cancellation | ACT-002 / ROLE-005 (whitelisted) | Order still before the cutoff defined by BR-006 (default: before `PACKING` begins) | Must satisfy BR-006 cutoff rule | Release reservation (if still held) / initiate any compensation needed for a paid-but-uncancelled order; write timeline entry | `OrderCancelled` |
| ORD-T07 | `PAID` | `PACKING` | Order Operations Admin (or automated fulfillment kick-off) starts fulfillment | ROLE-004 / ACT-004 | Order must currently be `PAID` | Must go through the formal transition API — no direct field write (BR-011) | Create/initialize `shipment` record; write timeline entry | `OrderStatusChanged` |
| ORD-T08 | `PACKING` | `SHIPPED` | Shipment dispatched (carrier/tracking assigned and dispatch confirmed) | ROLE-004 / ACT-004 | `shipment.status` reaches `SHIPPED` (Section 7) | Tracking data must already be attached (BR-011) | Update `shipment`; write timeline entry | `OrderShipped` |
| ORD-T09 | `SHIPPED` | `COMPLETED` | Delivery confirmed | ACT-004 / ROLE-004 | `shipment.status` reaches `DELIVERED` (Section 7) | — | Write timeline entry; order reaches terminal success state | `OrderDelivered` (drives this transition) |

**Forbidden transitions (explicit):**
- `CANCELLED → *` (any target) — forbidden absolutely (BR-001).
- `RESERVED → PACKING`, `RESERVED → SHIPPED`, `RESERVED → COMPLETED` — forbidden; intermediate states cannot be skipped (BR-007).
- `* → PAID` via any path other than ORD-T02 (i.e., manual/direct marking of `PAID`) — forbidden unless performed through the controlled reconciliation procedure of FR-039, which itself still results in ORD-T02 being executed, never a raw field write (BR-012).
- `PACKING → CANCELLED`, `SHIPPED → CANCELLED`, `COMPLETED → CANCELLED` — not defined as valid transitions in this specification; once `PACKING` has begun, customer self-cancellation (and Support-assisted cancellation) is no longer permitted (this is the concrete meaning of the BR-006 cutoff, pending final confirmation in Decision Register CQ-002).
- Any transition attempted by a role without `ORDER_STATE_TRANSITION` permission — forbidden (`AUTHORIZATION_FAILED`).

**State-transition idempotency requirement:** Re-processing the same underlying triggering event twice (e.g., the payment callback being redelivered, or an admin double-clicking "advance to PACKING") must not produce two timeline entries for the same logical transition and must not attempt an invalid re-transition; the second occurrence must be detected and treated as a no-op (or rejected as `BUSINESS_RULE_VIOLATION` if it would represent an invalid transition from the order's now-current state).

**Timeline/audit requirement:** Every transition listed above must produce exactly one `order_timeline_event` row (FR-029), and every transition performed by a back-office actor must additionally produce an `audit_log` row (BR-015).

---

## 7. Shipment State Machine

**Shipment States:** `PACKING`, `SHIPPED`, `DELIVERED`.

**Coupling to Order State Machine:** A `shipment` record is created only when the Order transitions `PAID → PACKING` (ORD-T07). The Shipment's own `PACKING → SHIPPED` transition is what allows and drives the Order's `PACKING → SHIPPED` transition (ORD-T08). The Shipment's `SHIPPED → DELIVERED` transition is what allows and drives the Order's `SHIPPED → COMPLETED` transition (ORD-T09). There is no independent order state corresponding to `DELIVERED`; delivery confirmation is the trigger, and `COMPLETED` is the resulting order state.

| Transition ID | From State | To State | Trigger | Actor/System | Preconditions | Guards | Side Effects | Event |
|---|---|---|---|---|---|---|---|---|
| SHP-T01 | `[none]` | `PACKING` | Order reaches `PAID` and fulfillment starts | ROLE-004 / ACT-004 | Order status is `PAID` | — | Create `shipment` record | (internal, coupled to `OrderStatusChanged`) |
| SHP-T02 | `PACKING` | `SHIPPED` | Carrier and tracking code assigned; dispatch confirmed | ROLE-004 | `carrier_name` and `tracking_code` must be set before this transition | Tracking data cannot be set while order/shipment is not yet at `PACKING` (BR-011) | Set `shipped_at`; drives ORD-T08 | `OrderShipped` |
| SHP-T03 | `SHIPPED` | `DELIVERED` | Delivery confirmed | ACT-004 / ROLE-004 | Shipment currently `SHIPPED` | — | Set `delivered_at`; drives ORD-T09 | `OrderDelivered` |

**Forbidden transitions:** Setting `carrier_name`/`tracking_code` or advancing shipment status while the associated order has not reached `PACKING` is forbidden (`BUSINESS_RULE_VIOLATION`, BR-011). There is no "skip to DELIVERED" capability from `PACKING` directly.

---

## 8. Inventory Reservation State Machine

**States:** `ACTIVE`, `CONSUMED`, `RELEASED`, `EXPIRED`.

A reservation is created in `ACTIVE` state when inventory is successfully reserved during order creation. `ACTIVE` is the only non-terminal reservation state.

| Transition ID | From     | To         | Trigger                                                        | Actor               | Preconditions                                                          | Side Effects                                                                                                             | Event                                         |
| ------------- | -------- | ---------- | -------------------------------------------------------------- | ------------------- | ---------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ | --------------------------------------------- |
| INV-T01       | `[none]` | `ACTIVE`   | Inventory reservation succeeds during order creation           | ACT-004             | Sufficient available inventory exists for all required SKU quantities  | Decrement available inventory; create `inventory_reservation`                                                            | `InventoryReserved`                           |
| INV-T02       | `ACTIVE` | `CONSUMED` | Order successfully transitions to `PAID` after payment success | ACT-004             | Reservation is `ACTIVE`; the Order's transition to `PAID` is valid     | Reserved inventory becomes committed to the paid order; the reservation is no longer releasable as an unpaid reservation | Coupled to `PaymentSucceeded`                 |
| INV-T03       | `ACTIVE` | `RELEASED` | Payment fails or the Order is cancelled before `PAID`          | ACT-004             | Reservation is `ACTIVE`                                                | Restore the reserved quantity to available inventory                                                                     | Coupled to `PaymentFailed` / `OrderCancelled` |
| INV-T04       | `ACTIVE` | `EXPIRED`  | Reservation/payment TTL is exceeded before successful payment  | ACT-004 (scheduled) | Reservation is `ACTIVE`; TTL has elapsed; Order has not reached `PAID` | Restore the reserved quantity to available inventory                                                                     | `PaymentTimeout`                              |

**Terminal States:** `CONSUMED`, `RELEASED`, and `EXPIRED` are terminal states for the reservation lifecycle.

**Forbidden Transitions:** `CONSUMED → RELEASED`, `CONSUMED → EXPIRED`, `RELEASED → ACTIVE`, `EXPIRED → ACTIVE`, and any other transition originating from a terminal reservation state are forbidden.

**Important Boundary:** `PACKING` does not control the inventory reservation lifecycle. The reservation becomes `CONSUMED` when the Order reaches `PAID`; `PACKING` belongs to the fulfillment lifecycle.


---

## 9. Payment Flow (full lifecycle)

The payment flow is a multi-step, cross-domain interaction; it is **not** merely "receive a callback." The full lifecycle is:

**Step 1 — Payment Initiation.** When an order reaches `RESERVED` (ORD-T01), the system must initiate a payment request toward the simulated Payment Gateway (ACT-005), carrying the order identifier, the `grand_total_amount`, and currency. A `payment_transaction` record is created in state `INITIATED` at this point (this is the "pending" state — the order remains `RESERVED` while payment is `INITIATED`).

**Step 2 — Payment Processing.** The Payment Gateway (simulated, external to this system's control) processes the request asynchronously and, at some later point, returns a result via callback (FR-010, API-PAY-001).

**Step 3 — Callback / Result Handling.** The callback must be authenticated (mechanism `TECH_STACK_DECISION_REQUIRED`) before being trusted. Based on the result:
- **SUCCESS** → `payment_transaction.status = SUCCEEDED`; drives ORD-T02.
- **FAILURE** → `payment_transaction.status = FAILED`; drives ORD-T03; triggers inventory compensation (FR-020).
- **No callback within the configured timeout window** → the system must independently detect this (it cannot wait indefinitely) and treat it as `payment_transaction.status = TIMEOUT`; drives ORD-T04; triggers inventory compensation.

**Step 4 — Duplicate Callback Handling.** If a callback is received for a `payment_transaction` that has already reached a terminal state (`SUCCEEDED`, `FAILED`, or `TIMEOUT`), the system must **not** reprocess it as if it were new. A duplicate `SUCCEEDED` callback for an order already `PAID` must be rejected from an order-transition standpoint and recorded as an `exception_record` (`type = DUPLICATE_PAYMENT`) for administrative visibility (BR-002).

**Step 5 — Late Callback Handling.** If a callback arrives referencing an order that has moved beyond the point where the callback is still applicable (e.g., the order already `EXPIRED` or `CANCELLED` by the time a late SUCCESS callback arrives), the system must not silently apply the payment result to change the order's state; instead it must record an `exception_record` (`type = LATE_OR_STALE_PAYMENT_CALLBACK`) so an administrator can review and, if warranted, perform a controlled manual reconciliation (Step 7).

**Step 6 — Idempotency.** The callback-processing operation must be idempotent with respect to the Payment Gateway's own retries of the same callback (i.e., if the gateway resends the identical result notification because it did not receive an acknowledgment, processing it twice must not create two `payment_transaction` records nor apply the order transition twice).

**Step 7 — Reconciliation (administrative, controlled).** For any case where an order's payment status appears mismatched with reality (e.g., a late/lost callback, a gateway anomaly), Order Operations Admin may view anomalous transactions (FR-039) and perform a manual reconciliation that **requires** an `evidence_reference` and a mandatory `reason`; this is the **only** path by which an order may be marked `PAID` outside the normal SUCCESS-callback path (BR-012), and it still results in the same ORD-T02 transition being executed through the same guarded mechanism — there is no separate "force PAID" field write.

**Step 8 — Interaction with Inventory Reservation.** Payment SUCCESS causes the associated `inventory_reservation` to move toward `CONSUMED` (INV-T02); payment FAILURE or TIMEOUT causes it to move to `RELEASED`/`EXPIRED` (INV-T03/INV-T04) — this is the compensating action of the overall checkout Saga (see Section 14, Saga/Compensation).

**Step 9 — Interaction with Order State Machine.** Every payment outcome maps to exactly one Order transition, as enumerated in Section 6 (ORD-T02, ORD-T03, ORD-T04).

**Payment Transaction State Machine:**

| Transition ID | From | To | Trigger | Guard |
|---|---|---|---|---|
| PAY-T01 | `[none]` | `INITIATED` | Order reaches `RESERVED`; payment request sent to gateway | — |
| PAY-T02 | `INITIATED` | `SUCCEEDED` | Gateway callback = SUCCESS | Only one `SUCCEEDED` transaction is allowed per order (BR-002); a second attempted SUCCESS is rejected, not transitioned |
| PAY-T03 | `INITIATED` | `FAILED` | Gateway callback = FAILURE | — |
| PAY-T04 | `INITIATED` | `TIMEOUT` | No callback received within the configured timeout window | Timeout duration: `NEEDS_CLARIFICATION` |

**Payment scope note:** The Payment Gateway referenced throughout this document is a **simulated** system (ASM-003); this specification does not select or integrate any real payment provider, and none should be introduced.

**Acceptance Criteria (payment flow, consolidated):**
```gherkin
Feature: Payment Processing
Scenario: Successful payment
  Given an order is RESERVED and a payment transaction is INITIATED
  When the gateway callback reports SUCCESS
  Then the payment transaction becomes SUCCEEDED
  And the order transitions to PAID
  And fulfillment initiation is triggered

Scenario: Failed payment
  Given an order is RESERVED and a payment transaction is INITIATED
  When the gateway callback reports FAILURE
  Then the payment transaction becomes FAILED
  And the order transitions to PAYMENT_FAILED
  And the reserved inventory is released

Scenario: Payment timeout
  Given an order is RESERVED and a payment transaction is INITIATED
  When no callback is received within the configured timeout window
  Then the payment transaction becomes TIMEOUT
  And the order transitions to EXPIRED
  And the reserved inventory is released

Scenario: Duplicate success callback
  Given an order is already PAID with a SUCCEEDED payment transaction
  When another SUCCESS callback is received for the same order
  Then the system does not create a second successful payment transaction
  And an exception record of type DUPLICATE_PAYMENT is created

Scenario: Late callback after order expired
  Given an order has already transitioned to EXPIRED
  When a SUCCESS callback arrives referencing that order
  Then the system does not change the order's state
  And an exception record of type LATE_OR_STALE_PAYMENT_CALLBACK is created

Scenario: Manual reconciliation without evidence
  Given an order's payment status is ambiguous
  When an admin attempts to mark the order PAID without supplying evidence and a reason
  Then the system rejects the request with BUSINESS_RULE_VIOLATION
```

---

## 10. Exception Record State Machine

**States:** `OPEN`, `RESOLVED`.

| Transition ID | From | To | Trigger | Actor |
|---|---|---|---|---|
| EXC-T01 | `[none]` | `OPEN` | An exception-generating condition occurs (duplicate payment, late callback, stuck order, notification failure) | ACT-004 (system-detected) |
| EXC-T02 | `OPEN` | `RESOLVED` | Admin reviews and marks resolved, with a mandatory note | ROLE-004 / ROLE-001 |

---

## 11. Logical Data Model

> Database engine, ORM, and physical schema syntax are `TECH_STACK_DECISION_REQUIRED`. Everything below is engine-agnostic logical modeling: every entity, field, type/domain, constraint, relationship, lifecycle behavior, and snapshot/mutability rule needed to implement the system is specified here.

**USER_ACCOUNT**
- Purpose: Identity record for both Customers and Back-office Users.
- Fields: `id` (identifier, required, unique, immutable); `email` (string, required, unique, format-validated); `password_hash` (string, required — must never store the plaintext password; hashing algorithm `TECH_STACK_DECISION_REQUIRED`); `account_type` (enum: `CUSTOMER` | `BACK_OFFICE`, required, immutable after creation); `status` (enum: `ACTIVE` | `LOCKED`, required, mutable, default `ACTIVE`); `full_name` (string, optional).
- Relationships: 1—N to `customer_address` (only when `account_type=CUSTOMER`); N—N to `role` via `user_role`; 1—N to `orders`, `audit_log` (as actor), `business_configuration` (as updater), `exception_record` (as resolver).
- Lifecycle: created at registration (FR-001); status mutated by lock/unlock (FR-018).
- Snapshot/Mutable: fully mutable except `id`, `account_type`.

**CUSTOMER_ADDRESS**
- Purpose: A customer's saved shipping address, one of which may be chosen at checkout.
- Fields: `id` (required, unique); `customer_id` (FK → user_account.id, required — must reference an account with `account_type=CUSTOMER`); `recipient_name`, `phone`, `line1`, `ward`, `district`, `city` (string, required); `line2` (string, optional); `is_default` (boolean, required, default `false` — at most one `true` per customer, enforced at the application level).
- Relationships: N—1 to `user_account`.
- Lifecycle: full CRUD by the owning customer (FR-022).
- Snapshot/Mutable: mutable. **Critically, this table is never referenced live by an order** — see `orders` snapshot fields below (BR-017).

**ROLE / PERMISSION / ROLE_PERMISSION / USER_ROLE**
- `role`: `id` (required, unique); `code` (enum: `SUPER_ADMIN` | `CATALOG_ADMIN` | `INVENTORY_ADMIN` | `ORDER_OPS_ADMIN` | `CUSTOMER_SUPPORT`, required, unique). Additional roles may be created via FR-033 (RBAC is not hardcoded to exactly these five at the schema level, even though these five are the confirmed business roles).
- `permission`: `id` (required, unique); `code` (string, required, unique — e.g., `PRODUCT_WRITE`, `ORDER_STATE_TRANSITION`, `CONFIG_WRITE`; the full permission-code catalog is given in Section 16).
- `role_permission`: composite key (`role_id`, `permission_id`); both FK required.
- `user_role`: composite key (`user_id`, `role_id`); both FK required; a user may hold multiple roles (N—N).
- Lifecycle: managed exclusively via FR-033. A new role starts with zero rows in `role_permission` (BR-018, deny-by-default).
- Snapshot/Mutable: mutable via RBAC administration only.

**CATEGORY**
- Purpose: Product categorization, at most two levels deep.
- Fields: `id` (required, unique); `parent_category_id` (FK → category.id, optional/self-referencing — **the application must reject creating a category under a parent that itself already has a parent**, enforcing the 2-level limit); `name` (string, required, unique); `slug` (string, required, unique); `status` (enum: `ACTIVE` | `INACTIVE`, required, default `ACTIVE`).
- Relationships: self 1—N (`parent_category_id`); 1—N to `product`.
- Lifecycle: FR-015.
- Snapshot/Mutable: mutable.

**PRODUCT**
- Purpose: Parent product record; not itself a sellable/inventory-tracked unit (BR-014).
- Fields: `id` (required, unique); `category_id` (FK → category.id, required); `name` (string, required); `description` (string, optional); `status` (enum: `ACTIVE` | `DISCONTINUED`, required, default `ACTIVE`).
- Relationships: N—1 to `category`; 1—N to `sku`; 1—N to `product_media`.
- Lifecycle: FR-014; hard delete disallowed if referenced by an incomplete order — must transition to `DISCONTINUED` instead (BR-005).
- Snapshot/Mutable: mutable, except values already copied into an `order_item` snapshot, which are unaffected by later product changes (BR-013).

**PRODUCT_ATTRIBUTE / PRODUCT_ATTRIBUTE_VALUE**
- `product_attribute`: `id` (required, unique); `name` (string, required, unique — e.g., "Color").
- `product_attribute_value`: `id` (required, unique); `attribute_id` (FK, required); `value` (string, required; unique together with `attribute_id`).
- Lifecycle: FR-024.
- Snapshot/Mutable: mutable.

**SKU**
- Purpose: The sellable variant of a product; the unit at which inventory, pricing, and cart/order lines operate (BR-014).
- Fields: `id` (required, unique); `product_id` (FK, required); `sku_code` (string, required, unique); `status` (enum: `ACTIVE` | `DISCONTINUED`, required, default `ACTIVE`).
- Relationships: N—1 to `product`; N—N to `product_attribute_value` via `sku_attribute_value`; 1—1 to `inventory`; 1—N to `inventory_reservation`, `price`, `price_promotion`, `cart_item`, `order_item` (soft reference only, see below).
- Lifecycle: FR-023. Every product must have at least one SKU; a product declaring no explicit variant receives exactly one default SKU (ASM-007, pending confirmation).
- Snapshot/Mutable: mutable; however, once referenced in an `order_item`, the order_item's own snapshot fields are what matter for historical display — the live `sku` record may later change or be discontinued without affecting historical orders.

**SKU_ATTRIBUTE_VALUE**
- Purpose: Defines the exact attribute-value combination identifying a SKU (e.g., Color=Red + Size=M).
- Fields: composite key (`sku_id`, `attribute_value_id`), both FK required.
- Constraint: the combination of attribute-values for a given `product_id` must be unique across its SKUs (no two SKUs of the same product may share the identical combination) — enforced at the application level (FR-023 exception flow).

**PRODUCT_MEDIA**
- Purpose: Images associated with a product and/or a specific SKU.
- Fields: `id` (required, unique); `product_id` (FK, required); `sku_id` (FK, optional — null means the media applies at the product level); `url` (string, required — storage mechanism `TECH_STACK_DECISION_REQUIRED`); `media_type` (enum, required); `sort_order` (integer, optional).
- Lifecycle: FR-025.

**PRICE**
- Purpose: Base price history for a SKU.
- Fields: `id` (required, unique); `sku_id` (FK, required); `base_price` (decimal, required, must be > 0); `currency` (string, required — ASM-001 implies a single currency system-wide); `effective_from` (timestamp, required); `effective_to` (timestamp, optional — null means currently active).
- Lifecycle: FR-026 — creating a new price closes the prior active record's `effective_to`.
- Snapshot/Mutable: historical rows are immutable once superseded; the "currently active" row is the only one further updatable (by being closed when superseded).

**PRICE_PROMOTION**
- Purpose: Time-limited promotional price for a SKU.
- Fields: `id` (required, unique); `sku_id` (FK, required); `sale_price` (decimal, required); `start_at`, `end_at` (timestamp, required, `start_at < end_at`); `status` (enum: `ACTIVE` | `INACTIVE`, required).
- Lifecycle: FR-026.

**INVENTORY**
- Purpose: Current stock balance for a SKU.
- Fields: `id` (required, unique); `sku_id` (FK, required, unique — 1—1 with SKU per BR-014); `quantity_on_hand` (integer, required, **must never be negative**); `quantity_reserved` (integer, required, derived/maintained by reservation logic); `quantity_available` (derived = `quantity_on_hand − quantity_reserved`; may be materialized or computed on read — either is acceptable at the logical level, but the value returned to callers must always reflect this formula).
- Lifecycle: FR-016, FR-019, FR-030.
- Snapshot/Mutable: mutable; every mutation via FR-030 must simultaneously produce an `inventory_adjustment_log` row.

**INVENTORY_RESERVATION**
- Purpose: Tracks a specific hold of stock against a specific order/SKU.
- Fields: `id` (required, unique); `sku_id` (FK, required); `order_id` (FK, required); `quantity` (integer, required, > 0); `status` (enum: `ACTIVE` | `CONSUMED` | `RELEASED` | `EXPIRED`, required); `expires_at` (timestamp, required — derived from the Business-Configuration-defined reservation TTL at the moment of creation).
- Lifecycle: Section 8.

**INVENTORY_ADJUSTMENT_LOG**
- Purpose: Immutable audit trail of every inventory quantity change.
- Fields: `id` (required, unique); `sku_id` (FK, required); `actor_id` (FK, required); `quantity_before`, `quantity_after`, `delta` (integer, required); `reason_code` (enum from a fixed set — e.g., restock, damaged, correction — required); `note` (string, optional); `created_at` (timestamp, required).
- Lifecycle: **insert-only; no update or delete operation exists for this entity, under any role** (BR-015, NFR-AUDIT-002).

**CART / CART_ITEM**
- `cart`: `id` (required, unique); `customer_id` (FK, required, unique — one active cart per customer); `status` (enum: `ACTIVE` | `CHECKED_OUT`, required).
- `cart_item`: `id` (required, unique); `cart_id` (FK, required); `sku_id` (FK, required); `quantity` (integer, required, > 0, and ≤ available stock at time of write — enforced at write-time, not as a static constraint).

**ORDERS**
- Purpose: The order aggregate — the central transactional record of a purchase.
- Fields:
  - `id` (required, unique, immutable)
  - `customer_id` (FK, required, immutable)
  - `status` (enum: `RESERVED` | `PAID` | `PACKING` | `SHIPPED` | `COMPLETED` | `PAYMENT_FAILED` | `EXPIRED` | `CANCELLED`, required, mutable only via the Section 6 state machine)
  - `idempotency_key` (string, required, **unique** — enforces BR-010)
  - Shipping-address snapshot fields (all required, all immutable once written): `shipping_recipient_name`, `shipping_phone`, `shipping_line1`, `shipping_line2` (optional), `shipping_ward`, `shipping_district`, `shipping_city`
  - `subtotal_amount` (decimal, required, ≥ 0, immutable once written)
  - `shipping_fee_amount` (decimal, required, ≥ 0, immutable once written)
  - `discount_amount` (decimal, required, = 0 in MVP per ASM-008, immutable once written)
  - `grand_total_amount` (decimal, required, immutable once written, must equal `subtotal_amount + shipping_fee_amount − discount_amount`)
  - `currency` (string, required, immutable)
  - `placed_at` (timestamp, required, immutable)
- Relationships: N—1 to `user_account`; 1—N to `order_item`, `order_timeline_event`, `inventory_reservation`, `payment_transaction`; 1—1 to `shipment`.
- Lifecycle: Section 6.
- Snapshot/Mutable: `status` is the only field that mutates after creation (via valid transitions only); every other field listed above is immutable once the order is created.

**ORDER_ITEM**
- Purpose: Immutable snapshot of a single purchased line.
- Fields: `id` (required, unique); `order_id` (FK, required); `sku_id` (FK, optional/soft reference only — used for lookup/statistics, **never** for display, since the SKU may later be discontinued or changed); `product_name_snapshot`, `sku_code_snapshot`, `attribute_snapshot`, (string, required, immutable); `unit_price_snapshot` (decimal, required, immutable); `quantity` (integer, required, > 0, immutable); `line_total` (decimal, required, immutable, = `unit_price_snapshot × quantity`).
- Lifecycle: created once, at order creation; **never updated or deleted thereafter.**

**ORDER_TIMELINE_EVENT**
- Purpose: Immutable, append-only record of every order status transition.
- Fields: `id` (required, unique); `order_id` (FK, required); `from_status` (enum, optional — null for the initial creation event); `to_status` (enum, required); `actor_id` (FK, optional — null for pure-system-driven transitions where no human actor is attributable); `actor_type` (enum: `CUSTOMER` | `BACK_OFFICE` | `SYSTEM`, required); `note` (string, optional); `occurred_at` (timestamp, required).
- Lifecycle: insert-only; **no update/delete API exists.**

**PAYMENT_TRANSACTION**
- Purpose: Records a single payment attempt for an order.
- Fields: `id` (required, unique); `order_id` (FK, required); `provider_reference` (string, optional — identifier from the simulated gateway); `amount` (decimal, required, must equal `orders.grand_total_amount`); `status` (enum: `INITIATED` | `SUCCEEDED` | `FAILED` | `TIMEOUT`, required); `attempted_at` (timestamp, required); `confirmed_at` (timestamp, optional — set when a terminal state is reached).
- Lifecycle: Section 9.

**SHIPMENT**
- Purpose: Simulated shipping/delivery record, one per order.
- Fields: `id` (required, unique); `order_id` (FK, required, unique — 1—1); `carrier_name`, `tracking_code` (string, optional until `PACKING` is reached, then required before `SHIPPED` may be reached); `status` (enum: `PACKING` | `SHIPPED` | `DELIVERED`, required); `packed_at`, `shipped_at`, `delivered_at` (timestamp, optional until each respective milestone is reached).
- Lifecycle: Section 7.

**NOTIFICATION_TEMPLATE**
- Fields: `id` (required, unique); `event_code` (string, required); `channel` (string, required — concrete channel type `NEEDS_CLARIFICATION`, CQ-006); `subject`, `body_template` (string, required); `status` (enum: `ACTIVE` | `INACTIVE`, required).

**NOTIFICATION_LOG**
- Fields: `id` (required, unique); `order_id` (FK, optional); `customer_id` (FK, optional); `template_id` (FK, required); `channel` (string, required); `status` (enum: `SENT` | `FAILED` | `RETRIED`, required); `attempt_count` (integer, required, default 0); `last_attempt_at` (timestamp, required).

**AUDIT_LOG**
- Purpose: Immutable, append-only record of every sensitive administrative action.
- Fields: `id` (required, unique); `actor_id` (FK, required); `actor_role` (string, required — the role held by the actor at the time of the action); `action_type` (enum, required — e.g., `ROLE_ASSIGN`, `CONFIG_CHANGE`, `INVENTORY_ADJUST`, `ORDER_STATE_TRANSITION`, `PAYMENT_RECONCILE`); `entity_type`, `entity_id` (required); `before_value`, `after_value` (optional, structured snapshot-style values); `reason` (string, required for actions where a reason is mandated — e.g., manual payment reconciliation per BR-012 — optional otherwise); `created_at` (timestamp, required).
- Lifecycle: **insert-only; no update or delete API exists, for any role including Super Admin** (NFR-AUDIT-002).

**BUSINESS_CONFIGURATION**
- Purpose: Time-versioned business parameter storage.
- Fields: `id` (required, unique); `config_key` (string, required — e.g., `RESERVATION_TTL_MINUTES`, `CANCELLATION_CUTOFF_STATUS`; unique combined with `effective_from`); `config_value` (string, required, type interpreted per `config_key`); `effective_from` (timestamp, required); `effective_to` (timestamp, optional — null means currently active); `updated_by` (FK, required, must hold `SUPER_ADMIN`).
- Lifecycle: FR-037; a change never mutates a past record's applicability — it closes the current record and opens a new one (BR-019).

**EXCEPTION_RECORD**
- Purpose: Aggregated, administrator-facing read model of business exceptions.
- Fields: `id` (required, unique); `type` (enum: `PAYMENT_FAILED` | `STUCK_ORDER` | `NOTIFICATION_FAILED` | `DUPLICATE_PAYMENT` | `LATE_OR_STALE_PAYMENT_CALLBACK`, required); `reference_entity_type`, `reference_entity_id` (required); `status` (enum: `OPEN` | `RESOLVED`, required); `detected_at` (timestamp, required); `resolved_by` (FK, optional); `resolved_at` (timestamp, optional); `resolution_note` (string, optional).
- Lifecycle: Section 10.

**Relational cardinalities (summary):** user_account 1—N customer_address; user_account N—N role (via user_role); role N—N permission (via role_permission); category 1—N category (self, max 2 levels); category 1—N product; product 1—N sku (min 1); sku N—N product_attribute_value (via sku_attribute_value); sku 1—1 inventory; sku 1—N inventory_reservation; sku 1—N price (history); orders 1—N order_item; orders 1—N order_timeline_event; orders 1—N payment_transaction (0 or 1 `SUCCEEDED`, BR-002); orders 1—1 shipment (created at `PACKING`); orders 1—N inventory_reservation.

---

## 12. API Specification

> Every endpoint below is complete: purpose, actor, authorization, preconditions, request, validation, success, errors, business rules, state changes, side effects, events, idempotency. HTTP status codes correspond to the Error Model in Section 15. Every authenticated endpoint requires an `Authorization` header carrying an access token (mechanism `TECH_STACK_DECISION_REQUIRED`) and a `X-Correlation-Id` header (required on every request per NFR-OBS-001). `Idempotency-Key` is required specifically on API-ORD-001.

**API-IAM-001 — Register Customer**
- Method/Path: `POST /api/v1/customers/register`
- Purpose: FR-001. Actor: ACT-001. Authentication: none. Authorization: none.
- Preconditions: email not already registered.
- Request Body: `{ email: string, password: string, full_name: string }`
- Validation: email format required; password minimum-strength rule — `NEEDS_CLARIFICATION` (no confirmed rule; must not be invented arbitrarily, see Decision Register).
- Success: 201, body `{ id, email, status }`.
- Errors: `VALIDATION_ERROR` (400), `CONFLICT` (409, duplicate email).
- Business Rules: NFR-SEC-001. State Changes: creates `user_account`. Side Effects: none. Events: none. Idempotency: not required (natural uniqueness via email).

**API-IAM-002 — Login**
- Method/Path: `POST /api/v1/auth/login`
- Purpose: FR-002. Actor: ACT-002/ACT-003. Auth: none.
- Request Body: `{ email: string, password: string }`
- Success: 200, body `{ access_token, expires_in, roles: string[] }`.
- Errors: `AUTHENTICATION_FAILED` (401), `AUTHORIZATION_FAILED` (403, account locked).
- Business Rules: BR-009 (enforced at checkout, not here). Side Effects: session/token issuance (mechanism TBD). Idempotency: not applicable.

**API-IAM-003 — Manage Customer Address**
- Methods/Paths: `POST /api/v1/customers/{customerId}/addresses`; `PUT /api/v1/customers/{customerId}/addresses/{addressId}`; `DELETE /api/v1/customers/{customerId}/addresses/{addressId}`; `GET /api/v1/customers/{customerId}/addresses`
- Purpose: FR-022. Actor: ACT-002 (owner only). Auth: required. Authorization: `customerId` must equal the authenticated subject.
- Request Body (POST/PUT): `{ recipient_name, phone, line1, line2?, ward, district, city, is_default: boolean }`
- Validation: all required fields non-empty; only one address may hold `is_default=true` per customer (enforced by unsetting any previous default when a new one is set).
- Success: 201/200/204 as appropriate. Errors: `VALIDATION_ERROR` (400), `AUTHORIZATION_FAILED` (403), `NOT_FOUND` (404).
- Business Rules: BR-017 (edits/deletes never retroactively affect existing orders — enforced by the fact that orders store their own snapshot, not a live reference).

**API-IAM-004 — Assign / Revoke Role**
- Methods/Paths: `POST /api/v1/backoffice/users/{userId}/roles` (assign); `DELETE /api/v1/backoffice/users/{userId}/roles/{roleId}` (revoke)
- Purpose: FR-018, FR-033. Actor: ROLE-001. Auth: required. Authorization: permission `USER_ROLE_MANAGE`.
- Request Body: `{ role_code: string }`
- Success: 200, body `{ user_id, roles: string[] }`.
- Errors: `AUTHORIZATION_FAILED` (403), `NOT_FOUND` (404), `BUSINESS_RULE_VIOLATION` (422, invalid role_code).
- Side Effects: writes `audit_log` (`action_type=ROLE_ASSIGN`). Business Rules: BR-005, BR-018.

**API-IAM-005 — Lock / Unlock User**
- Methods/Paths: `POST /api/v1/backoffice/users/{userId}/lock`; `POST /api/v1/backoffice/users/{userId}/unlock`
- Purpose: FR-018. Actor: ROLE-001. Authorization: `USER_ACCOUNT_MANAGE`.
- Success: 200. Errors: `AUTHORIZATION_FAILED` (403), `NOT_FOUND` (404). Business Rules: BR-005.

**API-CAT-001 — Browse Products by Category**
- Method/Path: `GET /api/v1/categories/{categoryId}/products?page=&size=`
- Purpose: FR-003. Actor: ACT-001/002. Auth: none.
- Success: 200, paginated `{ items: Product[], page, size, total }`. Errors: `NOT_FOUND` (404, category missing). NFR: NFR-PERF-001 (pagination required).

**API-CAT-002 — Search Products**
- Method/Path: `GET /api/v1/products/search?q=&page=&size=`
- Purpose: FR-004. Success: 200, paginated results (possibly empty). NFR: NFR-PERF-001.

**API-CAT-003 — Get Product Detail**
- Method/Path: `GET /api/v1/products/{productId}`
- Purpose: FR-005. Success: 200, body includes SKU list, effective price per SKU (per FR-026 rule), and available-stock status per SKU. Errors: `NOT_FOUND` (404). Business Rules: BR-004, BR-014.

**API-CAT-004 — Create / Update / Delete Product**
- Methods/Paths: `POST /api/v1/backoffice/products`; `PUT /api/v1/backoffice/products/{productId}`; `DELETE /api/v1/backoffice/products/{productId}`
- Purpose: FR-014. Actor: ROLE-002. Authorization: `PRODUCT_WRITE`.
- Errors: `AUTHORIZATION_FAILED` (403), `VALIDATION_ERROR` (400), `BUSINESS_RULE_VIOLATION` (422 — hard-delete blocked when referenced by an incomplete order; system must apply discontinuation instead). Business Rules: BR-005.

**API-CAT-005 — Create / Update Category**
- Methods/Paths: `POST /api/v1/backoffice/categories`; `PUT /api/v1/backoffice/categories/{categoryId}`
- Purpose: FR-015. Actor: ROLE-002. Errors: `VALIDATION_ERROR` (400 — duplicate name, or exceeds 2-level nesting). Business Rules: BR-005.

**API-CAT-006 — Manage SKU/Variant**
- Methods/Paths: `POST /api/v1/backoffice/products/{productId}/skus`; `PUT /api/v1/backoffice/skus/{skuId}`
- Purpose: FR-023. Actor: ROLE-002. Request Body: `{ sku_code, attribute_value_ids: id[], status }`. Errors: `BUSINESS_RULE_VIOLATION` (422, duplicate attribute combination). Business Rules: BR-014. Side Effects: initializes an `inventory` row for the new SKU.

**API-CAT-007 — Manage Attribute**
- Methods/Paths: `POST /api/v1/backoffice/attributes`; `POST /api/v1/backoffice/attributes/{attributeId}/values`
- Purpose: FR-024. Actor: ROLE-002. Errors: `VALIDATION_ERROR` (400, duplicate name/value). Business Rules: BR-014.

**API-CAT-008 — Manage Product/SKU Media**
- Method/Path: `POST /api/v1/backoffice/products/{productId}/media`
- Purpose: FR-025. Actor: ROLE-002. Storage mechanism: `TECH_STACK_DECISION_REQUIRED`. Errors: `VALIDATION_ERROR` (400, invalid format).

**API-PRC-001 — Set Base Price**
- Method/Path: `POST /api/v1/backoffice/skus/{skuId}/prices`
- Purpose: FR-026. Actor: ROLE-002. Request: `{ base_price, currency, effective_from }`. Side Effects: closes the prior active price's `effective_to`. Business Rules: BR-013.

**API-PRC-002 — Set Promotional Price**
- Method/Path: `POST /api/v1/backoffice/skus/{skuId}/promotions`
- Purpose: FR-026. Request: `{ sale_price, start_at, end_at }`. Errors: `VALIDATION_ERROR` (400, `start_at ≥ end_at`). Business Rules: BR-013.

**API-PRC-003 — Get Effective Price (internal)**
- Method/Path: `GET /api/v1/skus/{skuId}/effective-price`
- Purpose: FR-026/027/028 — used internally by the Order domain during checkout to determine and snapshot the applicable price at that instant. Not a customer-facing endpoint per se, but its behavior (the "effective price rule" in FR-026) is a binding requirement. Business Rules: BR-013.

**API-CART-001 — Add to Cart**
- Method/Path: `POST /api/v1/customers/{customerId}/cart/items`
- Purpose: FR-006. Actor: ACT-002. Request: `{ sku_id, quantity }`. Errors: `BUSINESS_RULE_VIOLATION` (422, exceeds available stock), `NOT_FOUND` (404, SKU missing). Business Rules: BR-004.

**API-CART-002 — Update / Remove Cart Item**
- Methods/Paths: `PUT /api/v1/customers/{customerId}/cart/items/{itemId}`; `DELETE /api/v1/customers/{customerId}/cart/items/{itemId}`
- Purpose: FR-007. Business Rules: BR-004.

**API-CART-003 — View Cart**
- Method/Path: `GET /api/v1/customers/{customerId}/cart`
- Purpose: FR-008.

**API-ORD-001 — Checkout (Create Order)**
- Method/Path: `POST /api/v1/customers/{customerId}/orders`
- Purpose: FR-009, FR-027, FR-028. Actor: ACT-002. Auth: required. Authorization: `customerId` must equal authenticated subject.
- Request Headers: `Idempotency-Key` (**required**).
- Preconditions: cart non-empty; address selected; every SKU has sufficient available stock at confirmation.
- Request Body: `{ address_id: id, cart_id: id }`
- Validation: cart must not be empty; address must belong to the caller.
- Success: 201 (new order created) or 200 (idempotent replay of a prior successful request with the same key) — body: `{ order_id, status: "RESERVED", subtotal_amount, shipping_fee_amount, discount_amount, grand_total_amount, items: OrderItem[] }`.
- Errors: `VALIDATION_ERROR` (400, empty cart), `BUSINESS_RULE_VIOLATION` (422, insufficient stock for one or more SKUs — identified in the error detail; also 422 for a reused idempotency key with a conflicting payload).
- Business Rules: BR-004, BR-009, BR-010, BR-013, BR-017.
- State Changes: creates `orders` (`RESERVED`), `order_item` rows, `inventory_reservation` rows (`ACTIVE`).
- Side Effects: computes monetary breakdown; snapshots address and items.
- Events: publishes `OrderCreated`.
- Idempotency: **required** — see FR-009 Alternative/Exception Flow for exact semantics.

**API-ORD-002 — Get Order Detail / Timeline**
- Method/Path: `GET /api/v1/customers/{customerId}/orders/{orderId}`
- Purpose: FR-011, FR-029. Authorization: `orders.customer_id` must equal authenticated subject → otherwise `AUTHORIZATION_FAILED` (403). Success: returns order + full `order_timeline_event` list.

**API-ORD-003 — Cancel Order (Customer)**
- Method/Path: `POST /api/v1/customers/{customerId}/orders/{orderId}/cancel`
- Purpose: FR-012. Errors: `INVALID_STATE` (409, past allowed cutoff), `BUSINESS_RULE_VIOLATION` (422, already `CANCELLED`). Business Rules: BR-001, BR-006, BR-007. Events: publishes `OrderCancelled`.

**API-ORD-004 — List / Search Orders (Admin)**
- Method/Path: `GET /api/v1/backoffice/orders?status=&customerId=&page=`
- Purpose: FR-017. Actor: ROLE-004. Authorization: `ORDER_READ`.

**API-ORD-005 — Transition Order Status (Admin)**
- Method/Path: `POST /api/v1/backoffice/orders/{orderId}/transitions`
- Purpose: FR-017, FR-029. Actor: ROLE-004. Authorization: `ORDER_STATE_TRANSITION`.
- Request Body: `{ target_status: string, note?: string }`
- Errors: `BUSINESS_RULE_VIOLATION` (422, invalid transition per Section 6, BR-001/007/011), `AUTHORIZATION_FAILED` (403).
- Side Effects: writes `order_timeline_event` and `audit_log`. Business Rules: BR-001, BR-007, BR-008, BR-011. Events: publishes `OrderStatusChanged` (and specific events per Section 6 where applicable, e.g. `OrderShipped`).

**API-ORD-006 — Stuck Order List**
- Method/Path: `GET /api/v1/backoffice/orders/stuck`
- Purpose: FR-042. Actor: ROLE-004.

**API-ORD-007 — Retry / Resume Stuck Order**
- Method/Path: `POST /api/v1/backoffice/orders/{orderId}/retry`
- Purpose: FR-042. Actor: ROLE-004. Effect: re-publishes the needed event(s); **does not** set order status directly (BR-011).

**API-PAY-001 — Payment Gateway Callback (simulated)**
- Method/Path: `POST /api/v1/payments/callback`
- Purpose: FR-010. Actor: ACT-005. Auth: signature/shared-secret mechanism — `TECH_STACK_DECISION_REQUIRED`; the requirement is that the callback **must be authenticated somehow** before being trusted.
- Request Body: `{ order_id, provider_reference, result: "SUCCESS"|"FAILED", amount }`
- Errors: `BUSINESS_RULE_VIOLATION` (422 — order not in a state where the callback is applicable, or duplicate SUCCESS per BR-002).
- Business Rules: BR-002, BR-003. Events: `PaymentSucceeded` / `PaymentFailed`. Idempotency: required (Section 9, Step 6).

**API-PAY-002 — Payment Reconciliation View**
- Method/Path: `GET /api/v1/backoffice/payments/anomalies?status=`
- Purpose: FR-039. Actor: ROLE-004. Business Rules: BR-012.

**API-PAY-003 — Manual Reconciliation Confirm**
- Method/Path: `POST /api/v1/backoffice/payments/{transactionId}/reconcile`
- Purpose: FR-039. Request Body: `{ evidence_reference: string, reason: string }` — **both mandatory** (BR-012). Errors: `BUSINESS_RULE_VIOLATION` (422, missing evidence/reason). Side Effects: mandatory `audit_log` entry; drives ORD-T02 through the standard transition mechanism, never a raw field write.

**API-FUL-001 — Update Shipment Info / Transition**
- Method/Path: `POST /api/v1/backoffice/orders/{orderId}/shipment`
- Purpose: FR-040. Actor: ROLE-004. Request Body: `{ carrier_name, tracking_code, target_status }`. Errors: `BUSINESS_RULE_VIOLATION` (422, tracking set before `PACKING`). Business Rules: BR-011. Events: `OrderShipped` / `OrderDelivered`.

**API-FUL-002 — Fulfillment Monitoring View**
- Method/Path: `GET /api/v1/backoffice/fulfillment?status=`
- Purpose: FR-021, FR-042.

**API-NOTI-001 — Manage Template**
- Methods/Paths: `POST /api/v1/backoffice/notification-templates`; `PUT /api/v1/backoffice/notification-templates/{templateId}`
- Purpose: FR-035. Actor: ROLE-001/ROLE-002.

**API-NOTI-002 — Delivery Log & Manual Retry**
- Methods/Paths: `GET /api/v1/backoffice/notifications?status=`; `POST /api/v1/backoffice/notifications/{logId}/retry`
- Purpose: FR-013, FR-036. Actor: ROLE-004.

**API-INV-001 — Adjust Inventory**
- Method/Path: `POST /api/v1/backoffice/skus/{skuId}/inventory/adjustments`
- Purpose: FR-016, FR-030. Actor: ROLE-003. Request: `{ delta: integer, reason_code: string, note?: string }`. Errors: `BUSINESS_RULE_VIOLATION` (422, resulting quantity negative). Side Effects: writes `inventory_adjustment_log` (immutable). Business Rules: BR-004, BR-015.

**API-INV-002 — Inventory Audit Trail Query**
- Method/Path: `GET /api/v1/backoffice/skus/{skuId}/inventory/audit-log`
- Purpose: FR-031. Business Rules: BR-015.

**API-RBAC-001 — Manage Role / Permission**
- Methods/Paths: `POST /api/v1/backoffice/roles`; `POST /api/v1/backoffice/roles/{roleId}/permissions`
- Purpose: FR-033. Actor: ROLE-001. Business Rules: BR-018.

**API-AUDIT-001 — Query Audit Log**
- Method/Path: `GET /api/v1/backoffice/audit-log?actor=&from=&to=&actionType=`
- Purpose: FR-034. Actor: ROLE-001. Read-only; no update/delete endpoint exists (NFR-AUDIT-002).

**API-CFG-001 — Get / Update Business Configuration**
- Methods/Paths: `GET /api/v1/backoffice/configurations`; `POST /api/v1/backoffice/configurations`
- Purpose: FR-037. Request Body: `{ config_key, config_value, effective_from }`. Actor: ROLE-001. Side Effects: mandatory `audit_log`. Business Rules: BR-019.

**API-EXC-001 — Exception Board**
- Methods/Paths: `GET /api/v1/backoffice/exceptions?status=`; `POST /api/v1/backoffice/exceptions/{exceptionId}/resolve`
- Purpose: FR-038. Actor: ROLE-004/ROLE-001.

**API-DASH-001 — Admin Dashboard Summary**
- Method/Path: `GET /api/v1/backoffice/dashboard/summary`
- Purpose: FR-032.

**API-SUP-001 — Customer Support Assisted Actions**
- Method/Path: `POST /api/v1/backoffice/customers/{customerId}/support-actions`
- Purpose: FR-041. Request Body: `{ action_type: "RESEND_NOTIFICATION"|"UNLOCK_ACCOUNT"|"INITIATE_CANCEL", order_id? }`. Actor: ROLE-005. Authorization: only whitelisted `action_type` values are accepted; any other value → `AUTHORIZATION_FAILED` (403). Business Rules: BR-016.

---

## 13. Internal Event Contracts

| Event | Producer (logical domain) | Consumers | Trigger | Required Payload Fields | Ordering Requirement | Idempotency | Failure Handling |
|---|---|---|---|---|---|---|---|
| `OrderCreated` | Order | Notification, Audit & Compliance | Order successfully created (ORD-T01) | order_id, customer_id, items[sku_id, quantity], idempotency_key | Must be published only after the order and its reservations are durably committed (transactional outbox guarantee) | Consumers must treat redelivery of the same event id as a no-op | If the message backbone is unavailable at publish time, the event must not be lost — publish must be retried until it succeeds (outbox pattern), even though the order transaction itself already committed |
| `InventoryReserved` | Inventory | Order | Reservation succeeds for a SKU line (INV-T01) | order_id, sku_id, quantity, reservation_id, status=ACTIVE | Must be correlated to the specific checkout request | Redelivery is a no-op | Retry with backoff on transient delivery failure |
| `InventoryReservationFailed` | Inventory | Order | Reservation cannot be completed for a SKU line (contention/out-of-stock) | order_id, sku_id, requested_quantity, reason | — | Redelivery is a no-op | — |
| `PaymentSucceeded` | Payment | Order, Inventory, Notification, Exception Management | Payment callback processed as SUCCESS (PAY-T02) | order_id, transaction_id, amount, status=SUCCEEDED | Must not be processed twice for the same order (BR-002) | Duplicate delivery for an already-`PAID` order must not re-apply the transition; must instead surface as an exception | Retry on transient delivery failure; the underlying business duplicate case is handled at the consumer (Order) level, not by suppressing redelivery |
| `PaymentFailed` | Payment | Order, Inventory, Notification | Payment callback processed as FAILURE (PAY-T03) | order_id, transaction_id, amount, status=FAILED | — | Redelivery is a no-op if the order has already left `RESERVED` | Retry on transient delivery failure |
| `PaymentTimeout` | Payment (system-detected) | Order, Inventory | No callback received within the timeout window (PAY-T04) | order_id, transaction_id, status=TIMEOUT | Must only fire once per transaction | Redelivery is a no-op if already processed | — |
| `OrderStatusChanged` | Order | Notification, Fulfillment, Audit & Compliance, Exception Management | Any order transition recorded in Section 6 | order_id, from_status, to_status, actor_id, actor_type, occurred_at | Consumers that build a timeline view must process these in the order they were produced per order_id | Redelivery must not create duplicate timeline entries downstream | Retry on transient delivery failure |
| `OrderCancelled` | Order | Inventory, Notification | ORD-T05 / ORD-T06 | order_id, cancelled_by_actor_id, cancelled_by_actor_type, occurred_at | — | Redelivery is a no-op | — |
| `OrderShipped` | Fulfillment | Order, Notification | SHP-T02 | order_id, tracking_code, carrier_name, occurred_at | Must occur after `PACKING` state confirmed | Redelivery is a no-op | — |
| `OrderDelivered` | Fulfillment | Order, Notification | SHP-T03 | order_id, occurred_at | Must occur after `SHIPPED` state confirmed | Redelivery is a no-op | — |
| `InventoryAdjusted` | Inventory | Audit & Compliance | FR-030 adjustment made | sku_id, actor_id, quantity_before, quantity_after, delta, reason_code, occurred_at | — | Redelivery must not create a duplicate audit-log entry | — |
| `NotificationDeliveryFailed` | Notification | Exception Management | A dispatch attempt fails | notification_log_id, reason, occurred_at | — | Redelivery is a no-op | — |
| `RoleAssigned` | Identity & Access | Audit & Compliance | FR-033 role assignment/revocation | actor_id, target_user_id, role_code, action (ASSIGN/REVOKE), occurred_at | — | Redelivery must not create a duplicate audit-log entry | — |
| `BusinessConfigChanged` | Business Configuration | Audit & Compliance | FR-037 configuration change | actor_id, config_key, before_value, after_value, effective_from, occurred_at | — | Redelivery must not create a duplicate audit-log entry | — |

**General event-contract requirement:** every event above must carry a correlation identifier so the originating request/transaction can be traced across every consuming service (NFR-OBS-001). The concrete message-transport technology (broker product, serialization format, schema registry) is `TECH_STACK_DECISION_REQUIRED`; CON-004 has already confirmed that the transport must be Apache Kafka, but message format/schema conventions are not decided here.

---

## 14. Distributed-System Requirements (observable behavior, not technology)

### 14.1 Idempotency
- **Operations requiring idempotency:** Checkout (API-ORD-001) and Payment Callback processing (API-PAY-001).
- **Idempotency key:** For checkout, a client-supplied `Idempotency-Key` header, scoped per customer. For payment callback, the gateway's own delivery-retry semantics must be handled such that redelivering the identical result does not double-apply it.
- **Duplicate request behavior:** A duplicate checkout request with the same key must return the result of the original operation (not create a second order). A duplicate payment callback for an already-terminal transaction must not reprocess it.
- **Conflicting payload behavior:** A checkout request reusing an idempotency key but with a materially different payload (different cart/address) must be rejected as `BUSINESS_RULE_VIOLATION`, not silently accepted as the original nor silently accepted as a new operation.
- **Expiration/retention:** How long an idempotency key must be honored before it can be reused/expired is **not yet confirmed** — `NEEDS_CLARIFICATION` (Decision Register).
- **Response behavior:** The replayed response must be functionally equivalent to the original successful response (same order id, same state).

### 14.2 Retry
- **Operations requiring retry:** synchronous calls from Order to Inventory and to Pricing during checkout; dispatch of notifications; delivery of asynchronous events to consumers.
- **Retry condition:** transient failures (timeouts, temporary unavailability of a dependency).
- **Non-retryable condition:** business-rule rejections (e.g., `BUSINESS_RULE_VIOLATION`, `VALIDATION_ERROR`) must never be retried automatically — retry applies only to infrastructure-level transient failures.
- **Maximum attempts / backoff strategy:** not yet confirmed — `NEEDS_CLARIFICATION` (NFR-RETRY-001).
- **Duplicate side-effect prevention:** every retried operation must be safe to repeat without creating duplicate business effects (e.g., retried inventory reservation calls must not double-reserve; this is guaranteed by the idempotency requirements in 14.1 combined with the atomic reservation guarantee in FR-019).

### 14.3 Timeout
- **Operations with defined timeout semantics:** the payment-result wait (Section 9, Step 3) — if no callback arrives within the configured window, the system must independently transition the order to `EXPIRED` (ORD-T04) rather than waiting indefinitely.
- **Resulting state:** `EXPIRED` order, `TIMEOUT` payment transaction, released inventory reservation.
- **Compensation:** inventory release (FR-020) is the compensating action for a payment timeout.
- **Concrete timeout duration:** not yet confirmed — `NEEDS_CLARIFICATION` (NFR-RESILIENCE-001/BR-003).

### 14.4 Circuit Breaker
- **Failure behavior:** when the Order domain's synchronous dependency (Inventory or Pricing) is failing repeatedly, the system must stop issuing further synchronous calls to that dependency for a cooldown period rather than continuing to fail on every request.
- **User-visible behavior:** during this cooldown, checkout attempts that depend on the failing dependency must fail fast with a `DEPENDENCY_ERROR` rather than hanging; browsing/search (which does not depend on Order Service) must remain unaffected (NFR-FAULTISO-001).
- **Fallback:** none defined beyond fail-fast; there is no degraded "best-effort" checkout path.
- **Recovery expectation:** after the cooldown period, the system must allow a limited number of trial requests through to detect recovery before resuming normal traffic. Exact thresholds (failure count, cooldown duration) not yet confirmed — `NEEDS_CLARIFICATION` (NFR-RESILIENCE-001).

### 14.5 Distributed Lock
- **Resource protected:** a SKU's available-stock counter during reservation (FR-019).
- **Why required:** to guarantee that two concurrent checkout requests contending for the last unit of stock cannot both succeed (prevents oversell — Edge Case EC-002).
- **Lock ownership:** held for the duration of the atomic reserve-and-decrement operation only.
- **Expiration/recovery semantics:** the lock must carry a TTL so that a crashed process holding the lock cannot block the resource indefinitely (Edge Case EC-008); exact TTL value not yet confirmed — `NEEDS_CLARIFICATION` (NFR-LOCK-001).
- **Behavior after lock-acquisition failure:** the requesting reservation attempt must fail gracefully (surfacing as insufficient-stock or a retryable transient error, not a hang) rather than corrupting the stock count.

### 14.6 Saga / Compensation
- **Business transaction:** Checkout-to-Fulfillment (Reserve Inventory → Await Payment → [Consume Inventory & Start Fulfillment] OR [Release Inventory & Fail/Expire Order]).
- **Participating domains:** Order, Inventory, Payment, Fulfillment, Notification.
- **Forward steps:** ORD-T01 (reserve) → PAY-T01/T02 (pay) → ORD-T07/SHP-T01 (pack) → SHP-T02/ORD-T08 (ship) → SHP-T03/ORD-T09 (deliver/complete).
- **Failure step:** payment fails or times out (PAY-T03/PAY-T04), or the customer/support cancels before the cutoff (ORD-T05/ORD-T06).
- **Compensating action:** release the inventory reservation (FR-020, INV-T03/INV-T04).
- **Eventual final state:** either `COMPLETED` (successful path) or one of `PAYMENT_FAILED` / `EXPIRED` / `CANCELLED` (compensated path) — every order must reach exactly one of these terminal states; there is no valid path that leaves inventory permanently reserved without a corresponding order outcome.

### 14.7 Eventual Consistency
- Order, Inventory, Notification, Fulfillment, and Audit & Compliance are updated via asynchronous events rather than a single distributed transaction; a brief propagation delay between these domains is acceptable. The maximum acceptable convergence window has not been confirmed — `NEEDS_CLARIFICATION` (NFR-CONSISTENCY-001).

### 14.8 Service Discovery / Load Balancing
- The system must support multiple running instances of each logical service and route requests among them without requiring hardcoded network addresses; this is required to satisfy the general architecture constraint (CON-002/CON-003) but the concrete mechanism is `TECH_STACK_DECISION_REQUIRED`.

### 14.9 Observability
- Every request and every asynchronous event must carry a correlation identifier that allows a single logical operation (e.g., one checkout) to be traced across every service and event hop it touches (NFR-OBS-001). Every error response must include this correlation identifier so it can be used for support/debugging.

---

## 15. Error Model

| Error Code | HTTP Status | Meaning | Trigger | Client Action | Retryable |
|---|---|---|---|---|---|
| `VALIDATION_ERROR` | 400 | Input failed field-level validation | Malformed/missing/out-of-range request data | Fix payload per returned detail | No |
| `AUTHENTICATION_FAILED` | 401 | Missing/invalid/expired credentials or token | Login failure, missing/expired token | Re-authenticate | No |
| `AUTHORIZATION_FAILED` | 403 | Caller lacks permission for this action or resource, or is not the resource owner | Insufficient role/permission; cross-account access attempt; whitelisted-action violation (BR-016) | Do not retry; escalate to an authorized actor | No |
| `NOT_FOUND` | 404 | Referenced resource does not exist or was removed | Unknown/deleted identifier | Verify identifier | No |
| `CONFLICT` | 409 | Data-level conflict (e.g., duplicate email) | Uniqueness violation | Refresh state and adjust request | Conditionally |
| `INVALID_STATE` | 409 | The requested action is not valid given the resource's current lifecycle state | E.g., cancel attempt on an order already past the cutoff | Check current state before retrying | No |
| `BUSINESS_RULE_VIOLATION` | 422 | A specific business rule (identified by ID, e.g. `BR-007`) was violated | Any BR/FR-guarded rejection | Read the violated rule and adjust the request; not a transient condition | No |
| `IDEMPOTENCY_REPLAY` | 200 | The request duplicates a previously completed idempotent operation | Same idempotency key resubmitted with an identical payload | Use the returned (original) result | N/A |
| `DEPENDENCY_ERROR` | 502/503 | A dependent internal service or external system (Payment Gateway, Notification Channel) failed or timed out | Downstream failure/timeout | The system retries per Section 14.2; client may poll for eventual status | Yes (system-managed) |
| `INTERNAL_ERROR` | 500 | Unclassified system-side failure | Unexpected condition | Retry later; report if persistent | Conditionally |

Every error response must include a `violated_rule` field (for `BUSINESS_RULE_VIOLATION`, referencing the specific BR/FR ID) and a `correlation_id` field (NFR-OBS-001) on every response, success or error.

---

## 16. Security Requirements

### 16.1 Authentication
- All endpoints require authentication except: product browsing/search/detail (API-CAT-001/002/003), registration (API-IAM-001), and login (API-IAM-002).
- Checkout requires an authenticated session (BR-009); guest checkout is not supported pending Decision Register CQ-001.
- The Payment Gateway callback endpoint requires its own authentication mechanism, distinct from user authentication — mechanism `TECH_STACK_DECISION_REQUIRED`, but the requirement that it **must be authenticated somehow before being trusted** is binding.
- Concrete token type/session mechanism is `TECH_STACK_DECISION_REQUIRED`.
- Passwords must never be stored in a reversible or plaintext form (NFR-SEC-001); the specific one-way hashing algorithm is `TECH_STACK_DECISION_REQUIRED`.

### 16.2 Authorization — Permission Catalog

| Permission Code | Meaning |
|---|---|
| `ADDRESS_SELF_WRITE` | Manage one's own address book |
| `CART_SELF_WRITE` | Manage one's own cart |
| `ORDER_SELF_WRITE` | Create/read/cancel one's own orders |
| `PRODUCT_WRITE` | Create/edit/delete product, category, SKU, attribute, media |
| `PRICING_WRITE` | Create/edit base price and promotional price |
| `INVENTORY_WRITE` | Adjust inventory quantities |
| `INVENTORY_AUDIT_READ` | Query inventory adjustment log |
| `ORDER_READ` | View orders in back-office |
| `ORDER_STATE_TRANSITION` | Transition an order's status through the state machine |
| `SHIPMENT_WRITE` | Update shipment info/status |
| `PAYMENT_RECONCILE_VIEW` | View payment anomalies and perform controlled reconciliation |
| `EXCEPTION_MANAGE` | View/resolve exception records |
| `USER_ROLE_MANAGE` | Assign/revoke roles and permissions |
| `CONFIG_WRITE` | Modify business configuration |
| `AUDIT_READ` | Query the audit log |
| `USER_ACCOUNT_MANAGE` | Lock/unlock user accounts |
| `CUSTOMER_SUPPORT_ACTION` | Perform the whitelisted Customer Support assisted actions only |

### 16.3 Authorization Matrix

| Actor/Role | Resource | Allowed Action | Permission |
|---|---|---|---|
| ACT-002 Customer | customer_address (own) | CRUD | `ADDRESS_SELF_WRITE` |
| ACT-002 Customer | cart (own) | Read/Write | `CART_SELF_WRITE` |
| ACT-002 Customer | orders (own) | Create/Read/Cancel | `ORDER_SELF_WRITE` |
| ROLE-002 Catalog Admin | product, category, sku, attribute, media, price | Create/Update/Delete | `PRODUCT_WRITE`, `PRICING_WRITE` |
| ROLE-003 Inventory Admin | inventory, inventory_adjustment_log | Adjust/Read | `INVENTORY_WRITE`, `INVENTORY_AUDIT_READ` |
| ROLE-004 Order Operations Admin | orders, shipment, payment_transaction (reconciliation), exception_record, fulfillment | Read/Transition/Reconcile (controlled) | `ORDER_READ`, `ORDER_STATE_TRANSITION`, `SHIPMENT_WRITE`, `PAYMENT_RECONCILE_VIEW`, `EXCEPTION_MANAGE` |
| ROLE-005 Customer Support | customer profile, orders (read), notification_log | Read + whitelisted actions only | `CUSTOMER_SUPPORT_ACTION` — **explicitly excludes** any monetary-write or inventory-write permission |
| ROLE-001 Super Admin | role, permission, user_role, business_configuration, audit_log, user_account | Full, within back-office administration scope | `USER_ROLE_MANAGE`, `CONFIG_WRITE`, `AUDIT_READ`, `USER_ACCOUNT_MANAGE` |
| ACT-004 System | inventory_reservation, order_timeline_event, notification_log | Write, via internal service-to-service trust, not via any public API | (internal trust, not a public permission) |
| ACT-005 Payment Gateway | payment_transaction | Callback write only | dedicated callback authentication, not a user permission |

### 16.4 Binding Security Principles
1. **Deny-by-default:** a newly created role has zero permissions until explicitly granted (BR-018).
2. **No bypass of state machines:** no role, including Super Admin, may hold a permission that allows setting Order/Shipment/Payment status directly outside the formal transitions defined in Sections 6/7/9 (BR-011).
3. **Customer isolation:** a customer may only read/write resources (`orders`, `cart`, `customer_address`) that belong to their own account.
4. **Admin least-privilege isolation:** each back-office role's permission set is scoped exactly as shown in 16.3; no role implicitly inherits another role's permissions.
5. **`PAYMENT_RECONCILE_VIEW`** grants read access plus the ability to trigger the controlled reconciliation procedure (FR-039) — it is never a free-form "set PAID" capability (BR-012).
6. **Sensitive-action audit:** every write performed under `PRODUCT_WRITE`, `PRICING_WRITE`, `INVENTORY_WRITE`, `ORDER_STATE_TRANSITION`, `USER_ROLE_MANAGE`, `CONFIG_WRITE`, `USER_ACCOUNT_MANAGE`, and `PAYMENT_RECONCILE_VIEW` (reconciliation action) must produce an `audit_log` entry (BR-015).

---

## 17. Non-Functional Requirements

| ID | Category | Requirement | Metric | Target | Status | Verification |
|---|---|---|---|---|---|---|
| NFR-PERF-001 | Performance | Response time for product browse/search | p95 latency | Not set | `NEEDS_CLARIFICATION` | Performance test once target is set |
| NFR-AVAIL-001 | Availability | System uptime | % uptime | Not set | `NEEDS_CLARIFICATION` | Uptime monitoring once target is set |
| NFR-SCALE-001 | Scalability | Concurrent users/orders supported | count | Not set | `NEEDS_CLARIFICATION` | Load test once target is set |
| NFR-CONSISTENCY-001 | Consistency | Max convergence window for eventual consistency across services | duration | Not set | `NEEDS_CLARIFICATION` | Consumer-lag/chaos test once target is set |
| NFR-RESILIENCE-001 | Resilience | Circuit breaker opens after N consecutive failures within window T, half-opens after cooldown | N, T, cooldown | Not set | `NEEDS_CLARIFICATION` | Fault-injection test once values are set |
| NFR-RETRY-001 | Resilience | Retry policy for transient failures | max attempts, backoff strategy | Not set | `NEEDS_CLARIFICATION` | Fault-injection test once policy is set |
| NFR-IDEMPOTENCY-001 | Reliability | Idempotency-key handling for state-changing requests | at-most-once effect within a retention window | Retention window not set | `NEEDS_CLARIFICATION` | Duplicate-request test |
| NFR-FAULTISO-001 | Fault Isolation | Catalog browse/search must remain available when Order Service is unavailable | pass/fail | Must hold true | `CONFIRMED` | Chaos test: disable Order Service, verify browse/search still works |
| NFR-SEC-001 | Security | Passwords never stored in plaintext | pass/fail | Must hold true; algorithm TBD | `CONFIRMED` (requirement) / `TECH_STACK_DECISION_REQUIRED` (algorithm) | Security review |
| NFR-SEC-002 | Security | Every back-office write endpoint enforces authorization | % endpoints covered | 100% | `CONFIRMED` | Access-control test against Section 16.3 |
| NFR-AUDIT-001 | Auditability | Every order status change records actor + timestamp; minimum retention period | retention duration | Not set | `NEEDS_CLARIFICATION` | Audit query test once retention is set |
| NFR-OBS-001 | Observability | Correlation ID present on every request/event | % coverage | 100% | `CONFIRMED` | Distributed tracing verification |
| NFR-TEST-001 | Quality | Minimum test coverage | % coverage | Not set | `NEEDS_CLARIFICATION` | Coverage report once target is set |
| NFR-DEPLOY-001 | Deployability | Every service containerized with a build/test/deploy pipeline | pass/fail | Must hold true | `CONFIRMED` (requirement) / `TECH_STACK_DECISION_REQUIRED` (tooling) | CI/CD pipeline execution log |
| NFR-LOCK-001 | Concurrency | Distributed lock on inventory reservation must have a TTL | TTL value | Not set | `NEEDS_CLARIFICATION` | Concurrency test (two orders contend for the last unit) once TTL is set |
| NFR-AUDIT-002 | Auditability | `audit_log`, `inventory_adjustment_log`, `order_timeline_event` are append-only; no update/delete capability exists | pass/fail | Must hold true, no exception for any role | `CONFIRMED` | Attempt update/delete via API, must be rejected/non-existent |
| NFR-RBAC-001 | Security | Authorization checks use specific permission codes, never a generic "is_admin" flag | pass/fail | 100% of sensitive actions | `CONFIRMED` | Access-control test per permission |
| NFR-CONFIG-001 | Availability | Business Configuration changes take effect for new transactions without disrupting in-flight ones | pass/fail; degree of "no disruption" | Not fully specified | `NEEDS_CLARIFICATION` | Test: change config while a transaction is open, confirm no retroactive effect |

**Explicit statement per Rule 12:** No numeric target has been invented for any NFR marked `NEEDS_CLARIFICATION` above. Implementation must not assume a default value for these; if a value is required to proceed, it must be obtained via the Decision Register (Section 20) resolution process, not fabricated.

---

## 18. Edge Cases

| ID | Edge Case | Related Requirement |
|---|---|---|
| EC-001 | Payment Gateway timeout/non-response during payment | Section 9, ORD-T04 |
| EC-002 | Two orders contend for the last unit of stock | FR-019, Section 14.5 |
| EC-003 | Customer requests cancellation immediately after payment success but before packing begins | ORD-T06 |
| EC-004 | Duplicate order creation via double-submit/client retry | FR-009, BR-010, Section 14.1 |
| EC-005 | Message backbone temporarily unavailable when `OrderCreated` needs to be published | Section 14.6 (Outbox) |
| EC-006 | Order "abandoned" at the payment-pending step | ORD-T04, FR-020 |
| EC-007 | Admin attempts to delete/deactivate a product still referenced by an incomplete order | FR-014, BR-005 |
| EC-008 | Distributed lock on inventory not released due to a mid-operation process crash | Section 14.5 |
| EC-009 | Circuit breaker open on the Payment-dependent path — customer-visible behavior during this window | Section 14.4; concrete UX not specified — `NEEDS_CLARIFICATION` |
| EC-010 | Session/token expires mid-checkout | FR-002, FR-009 |
| EC-011 | Notification Channel unavailable | FR-013 |
| EC-012 | Two admins concurrently update the same product (lost update) | FR-014 |
| EC-013 | Admin attempts to skip an intermediate order state | Section 6, BR-007, BR-011 |
| EC-014 | Product deleted/discontinued after being ordered; historical order must still display correctly via snapshot | FR-028, BR-013 |
| EC-015 | Two back-office users (different roles) act on the same order concurrently | Section 6 |
| EC-016 | Product price changes exactly while a customer is checking out | FR-026/027/028, BR-013 |
| EC-017 | Customer Support attempts an action outside the whitelist | FR-041, BR-016 |
| EC-018 | Business Configuration value changes while an order is holding a reservation created under the old value | FR-037, BR-019 |
| EC-019 (new, surfaced by Payment Flow completion) | A payment success callback arrives late, after the order has already expired | Section 9, Step 5 |
| EC-020 (new, surfaced by Payment Flow completion) | A duplicate success callback is redelivered by the gateway for an already-`PAID` order | Section 9, Step 4, BR-002 |

---

## 19. Scope Boundaries

See Section 1.2 for the authoritative in-scope/out-of-scope table. No feature not already present in the source requirements has been added by this refactor; conversely, no in-scope feature has been dropped in the process of making this document standalone.

---

## 20. Decision Register (Open Requirements)

| ID | Issue | Affected Requirement | Impact | Status | Required Decision |
|---|---|---|---|---|---|
| CQ-001 | Is guest checkout permitted? | BR-009, FR-009, API-ORD-001 | Determines whether checkout requires authentication | `BLOCKING` | Confirm yes/no; current default in this document is "no" (ASM-006) |
| CQ-002 | Exact cutoff state(s) permitting customer self-cancellation, and the complete valid transition set (Section 6) | BR-006, BR-007, FR-012, Section 6 | Directly shapes the Order State Machine's forbidden-transition list | `BLOCKING` | Confirm exact cutoff; current default used throughout this document is "before `PACKING`" |
| CQ-003 / CQ-013 | Numeric NFR targets and default Business Configuration values (reservation TTL, cancellation cutoff duration, retry/backoff parameters, circuit-breaker thresholds, lock TTL, timeout duration) | BR-003, NFR-RESILIENCE-001, NFR-RETRY-001, NFR-LOCK-001, NFR-CONFIG-001, Section 14 | Without these, several behaviors cannot be finalized numerically (though the qualitative behavior is fully specified) | `BLOCKING` | Confirm concrete numeric values, or explicitly delegate to Business Configuration with agreed defaults |
| CQ-006 | Concrete notification channel type(s) (email/SMS/in-app/push) | EXT-004, `notification_template.channel` | Affects Notification Service contract shape | `NON_BLOCKING` | Confirm channel type(s) |
| CQ-008 | Required fields and minimum retention period for the audit log | NFR-AUDIT-001, `audit_log` | Affects compliance/retention design | `NON_BLOCKING` | Confirm retention period |
| CQ-010 | Should a dedicated "Payment Admin" role exist separately from Order Operations Admin? | Section 2.3.1, Section 16 | Would add a 6th role and split `PAYMENT_RECONCILE_VIEW` out | `NON_BLOCKING` | Confirm; current default is "no, kept merged into Order Operations Admin" |
| CQ-011 | Must every Product declare at least one explicit variant, or is a single-SKU product (no declared variant) allowed with an auto-created default SKU? | ASM-007, FR-023, `sku` entity | Affects Catalog data-entry UX and validation rules | `NON_BLOCKING` | Confirm; current default is "single-SKU allowed, auto-default" |
| CQ-012 | Should Grand Total include tax? | ASM-008, FR-027, `orders` monetary fields | Affects the monetary breakdown formula | `NON_BLOCKING` | Confirm; current default is "no tax in MVP" |
| CQ-014 | Formal roadmap classification confirmation for Coupon/Return/Review (Phase 2/3) | Section 1.2 | Confirms these remain out of MVP scope | `NON_BLOCKING` | Confirm phase classification is accepted |
| CQ-015 | Should Category support more than two levels of nesting? | `category.parent_category_id`, FR-015 | Affects Catalog data model depth | `NON_BLOCKING` | Confirm; current default is "maximum two levels" |
| CQ-016 (new) | What is the shipping-fee calculation rule? | FR-027, `orders.shipping_fee_amount` | No rule for computing this field has ever been confirmed in any prior artifact; a placeholder or fixed rule must not be invented silently | `BLOCKING` | Confirm the shipping fee calculation rule (flat fee, weight-based, free-shipping threshold, etc.) |
| CQ-017 (new) | What is the exact stuck-order detection threshold? | FR-042 | Cannot implement automated detection without a threshold | `NON_BLOCKING` | Confirm threshold duration |
| CQ-018 (new) | What is the required Payment Gateway callback authentication mechanism (business-level requirement: "must be authenticated"; the specific mechanism is a technology decision, but whether it is signature-based, secret-based, or mTLS-based may itself carry a business/security requirement not yet stated) | API-PAY-001, Section 16.1 | Affects Payment Service's callback contract | `TECH_STACK_DECISION_REQUIRED` (with a possible security-policy sub-question that is `NON_BLOCKING` until raised) | Confirm mechanism |
| Gap-EMERGENCY-OVERRIDE | Should Super Admin ever have an "emergency override" capability to force-resolve an order/payment/inventory state outside the normal transitions, for cases the normal exception process cannot resolve? | BR-011, BR-012, Section 6 | If such a capability is introduced, it must still be modeled as a specifically whitelisted, audited transition — never a raw field write. As of this document, **no such capability exists.** | `BLOCKING` if the business decides it is needed; otherwise resolved as "no such capability" | Confirm whether this capability is required; if yes, define it as an additional named transition with explicit guards, not a bypass |
| TECH-01 | Database engine per service | Section 2.5 | Affects Section 11 physical realization | `TECH_STACK_DECISION_REQUIRED` | Tech Stack Agent decision |
| TECH-02 | ORM/persistence framework | Section 2.5 | — | `TECH_STACK_DECISION_REQUIRED` | Tech Stack Agent decision |
| TECH-03 | Authentication token mechanism | Section 16.1 | — | `TECH_STACK_DECISION_REQUIRED` | Tech Stack Agent decision |
| TECH-04 | Payment callback authentication mechanism | API-PAY-001 | — | `TECH_STACK_DECISION_REQUIRED` | Tech Stack Agent decision |
| TECH-05 | Media storage mechanism | API-CAT-008 | — | `TECH_STACK_DECISION_REQUIRED` | Tech Stack Agent decision |
| TECH-06 | Testing framework/tools | Section 2.5 | — | `TECH_STACK_DECISION_REQUIRED` | Tech Stack Agent decision |
| TECH-07 | CI/CD platform | Section 2.5 | — | `TECH_STACK_DECISION_REQUIRED` | Tech Stack Agent decision |
| TECH-08 | Password hashing algorithm | NFR-SEC-001 | — | `TECH_STACK_DECISION_REQUIRED` | Tech Stack Agent decision |

**Contradiction check:** No unresolved contradiction between requirements was found in the source material during this refactor. All ambiguities above are gaps (missing decisions), not conflicts between stated requirements.

---

## 21. Traceability Matrix

> Format: RAW Domain Area → US → FR → BR → Entity → API → Event → NFR → Test Intent.

| RAW Domain Area | US | FR | BR | Entity | API | Event | NFR | Test Intent |
|---|---|---|---|---|---|---|---|---|
| Registration/Login | US-001 | FR-001 | — | user_account | API-IAM-001 | — | NFR-SEC-001 | Register success/duplicate/validation |
| Registration/Login | US-002 | FR-002 | BR-009 | user_account | API-IAM-002 | — | NFR-SEC-001, NFR-SEC-002 | Login success/wrong credentials/locked |
| Browse/Search | US-003 | FR-003 | — | category, product | API-CAT-001 | — | NFR-PERF-001 | Category with/without products |
| Browse/Search | US-004 | FR-004 | — | product | API-CAT-002 | — | NFR-PERF-001 | Search with/without results |
| Browse/Search | US-005 | FR-005 | BR-004, BR-014 | product, sku, inventory | API-CAT-003 | — | NFR-PERF-001 | Detail in-stock/not-found |
| Cart | US-006 | FR-006 | BR-004 | cart_item, sku | API-CART-001 | — | — | Add within/over stock limit |
| Cart | US-007 | FR-007 | BR-004 | cart_item | API-CART-002 | — | — | Update/remove cart line |
| Cart | US-008 | FR-008 | — | cart, cart_item | API-CART-003 | — | — | View cart |
| Checkout | US-009 | FR-009, FR-019, FR-027, FR-028 | BR-004, BR-009, BR-010, BR-013, BR-017 | orders, order_item, inventory_reservation | API-ORD-001 | OrderCreated, InventoryReserved | NFR-IDEMPOTENCY-001, NFR-LOCK-001 | Happy path/empty cart/insufficient stock/idempotent replay |
| Payment | US-010 | FR-010 | BR-002, BR-003 | payment_transaction, orders | API-PAY-001 | PaymentSucceeded, PaymentFailed, PaymentTimeout | NFR-IDEMPOTENCY-001, NFR-RETRY-001 | Success/failure/timeout/duplicate/late callback |
| Order Tracking | US-011 | FR-011, FR-029 | BR-008 | orders, order_timeline_event | API-ORD-002 | — | NFR-AUDIT-001 | View timeline/authorization failure |
| Cancellation | US-012 | FR-012, FR-020 | BR-001, BR-006, BR-007 | orders, inventory_reservation | API-ORD-003 | OrderCancelled | — | Valid cancel/invalid state/already cancelled |
| Notification | US-013 | FR-013 | — | notification_log, notification_template | API-NOTI-002 | OrderStatusChanged (consumed), NotificationDeliveryFailed | — | Send success/channel failure does not block order |
| Catalog Admin | US-014 | FR-014 | BR-005 | product | API-CAT-004 | — | NFR-SEC-002 | CRUD/authorization/blocked hard-delete |
| Catalog Admin | US-015 | FR-015 | BR-005 | category | API-CAT-005 | — | NFR-SEC-002 | Create category/duplicate name |
| Inventory Admin | US-016 | FR-016 | BR-004, BR-005 | inventory | API-INV-001 | — | NFR-SEC-002 | Adjust/boundary negative |
| Order Ops Admin | US-017, US-032 | FR-017, FR-029 | BR-001, BR-007, BR-008, BR-011 | orders, order_timeline_event | API-ORD-004, API-ORD-005 | OrderStatusChanged | NFR-AUDIT-001, NFR-RBAC-001 | Valid transition/invalid transition rejected |
| Super Admin | US-018 | FR-018 | BR-005, BR-018 | user_account, role, user_role | API-IAM-004, API-IAM-005 | RoleAssigned | NFR-SEC-002 | Lock/unlock/authorization |
| System | US-019 | FR-019 | BR-004, BR-014 | inventory, inventory_reservation | (internal) | InventoryReserved, InventoryReservationFailed | NFR-LOCK-001 | Atomic reserve/contention on last unit |
| System | US-020 | FR-020 | BR-003 | inventory_reservation, inventory | (internal) | (coupled to PaymentFailed/PaymentTimeout) | NFR-RESILIENCE-001, NFR-RETRY-001 | Release on failure/expiry |
| Fulfillment | US-021, US-042 | FR-021, FR-042 | BR-007, BR-011 | shipment, orders | API-FUL-002, API-ORD-006, API-ORD-007 | — | NFR-OBS-001 | Monitor pipeline/detect stuck/retry without forced state |
| Address Book | US-023 | FR-022 | BR-017 | customer_address | API-IAM-003 | — | — | Order with saved address/edit does not affect past order |
| Catalog extension | US-026 | FR-023, FR-024, FR-025 | BR-014 | sku, product_attribute, product_attribute_value, sku_attribute_value, product_media | API-CAT-006, API-CAT-007, API-CAT-008 | — | — | Create variant/duplicate combination rejected |
| Pricing | US-030 | FR-026 | BR-013 | price, price_promotion | API-PRC-001, API-PRC-002, API-PRC-003 | — | — | Promotional price applies going forward/past order unaffected |
| Inventory Audit | US-031 | FR-030, FR-031 | BR-015 | inventory, inventory_adjustment_log | API-INV-001, API-INV-002 | InventoryAdjusted | NFR-AUDIT-002 | Adjust with reason/audit immutable |
| Payment Reconciliation | US-033 | FR-039 | BR-012 | payment_transaction | API-PAY-002, API-PAY-003 | — | — | View anomalies/reject manual PAID without evidence |
| RBAC | US-034 | FR-033 | BR-018 | role, permission, role_permission, user_role | API-RBAC-001 | RoleAssigned | NFR-RBAC-001 | Assign role/new role deny-by-default |
| Customer Support | US-035, US-041 | FR-041 | BR-016 | user_account, notification_log, orders | API-SUP-001 | — | NFR-RBAC-001 | Whitelisted action succeeds/non-whitelisted rejected |
| Audit Log | US-036 | FR-034 | BR-015 | audit_log | API-AUDIT-001 | — | NFR-AUDIT-002 | Query by actor/time; no update/delete |
| Notification Template | US-037 | FR-035 | — | notification_template | API-NOTI-001 | — | — | Update template does not affect sent notifications |
| Business Config | US-038 | FR-037 | BR-019 | business_configuration | API-CFG-001 | BusinessConfigChanged | NFR-CONFIG-001 | Change is not retroactive |
| Exception Board | US-039 | FR-038 | — | exception_record | API-EXC-001 | — | — | View/resolve exceptions |
| Fulfillment Admin | US-040 | FR-040 | BR-011 | shipment | API-FUL-001 | OrderShipped, OrderDelivered | — | Transition shipment valid/blocked before PACKING |

---

## 22. Quality Gate

| Gate | Status | Notes |
|---|---|---|
| Business scope coverage | PASS | All in-scope capabilities from prior artifacts are represented; no feature added or removed |
| FR coverage | PASS | FR-001 through FR-042 fully specified with all 12 required fields each |
| BR coverage | PASS | BR-001 through BR-019 fully stated and mapped into state machines, APIs, and error handling |
| State Machine completeness | PASS | Order, Shipment, Inventory Reservation, Payment Transaction, and Exception Record state machines are fully specified with transition tables (from/to/trigger/actor/preconditions/guards/side effects/event); the Order↔Shipment relationship ambiguity (SHIPPED vs DELIVERED vs COMPLETED) has been explicitly resolved in Section 6 |
| Payment flow completeness | PASS | Full 9-step lifecycle specified (initiation, processing, callback, duplicate, late callback, idempotency, reconciliation, inventory interaction, order-state interaction) — not callback-only |
| Data completeness | PASS | Every entity referenced anywhere in this document is fully defined in Section 11 with fields, types, constraints, relationships, lifecycle, and snapshot/mutability rules |
| API completeness | PASS | All 39 endpoints specified with purpose, actor, authorization, preconditions, request, validation, success, errors, business rules, state changes, side effects, events, and idempotency where applicable |
| Event contract completeness | PASS | 13 internal events fully specified with producer, consumers, trigger, payload, ordering, idempotency, and failure handling |
| Security coverage | PASS | Full authentication requirements, permission catalog, and authorization matrix defined; deny-by-default and no-bypass principles explicitly stated |
| NFR coverage | PASS (qualitative) / PARTIAL (quantitative) | Every NFR has a defined category, requirement statement, and status; several lack confirmed numeric targets, explicitly marked `NEEDS_CLARIFICATION`, not invented |
| Traceability | PASS | Full RAW→US→FR→BR→Entity→API→Event→NFR→Test-Intent matrix provided in Section 21 |
| Standalone validation | PASS | See Section 23 |
| External-document dependency | PASS | No reference to any external artifact remains anywhere in this document |
| Technology leakage | PASS | No technology (database engine, framework beyond the already-confirmed CON-001/002/003/004/005/007, ORM, auth library, CI/CD tool) has been selected; all such items are explicitly marked `TECH_STACK_DECISION_REQUIRED` |
| Blocking decisions | OPEN | 4 items marked `BLOCKING` in Section 20 (CQ-001, CQ-002, CQ-003/CQ-013, CQ-016) plus the conditional Gap-EMERGENCY-OVERRIDE must be resolved before the affected requirements can be considered final for code generation; all other requirements are implementable as specified |

---

## 23. Standalone Validation

| Check | Result |
|---|---|
| If all prior Business Analysis / System Design artifacts were deleted, is this SRS still fully understandable? | YES |
| Is there any remaining API listed without full behavior? | NO |
| Is there any entity referenced but not defined? | NO |
| Is there any event referenced but without a contract? | NO |
| Is there any state transition referenced but not defined? | NO |
| Is the payment flow fully specified beyond just the callback? | YES — Section 9 covers initiation through reconciliation |
| Is there any business rule that contradicts the state machine? | NO |
| Is there any requirement hidden behind "see other document"? | NO |
| Has any technology decision been made without authorization? | NO — all technology items are `TECH_STACK_DECISION_REQUIRED`, except the nine already-confirmed constraints in Section 2.5, which were confirmed in the source Business Analysis, not invented here |
| Has any Open Question / CQ been silently resolved? | NO — every CQ retains its "pending confirmation" status in Section 20; where a "default" is used elsewhere in the document for concreteness (e.g., cancellation cutoff = before PACKING), it is explicitly marked as a default pending confirmation, not treated as final |
| Can a System Design Agent read only this document and begin architecture work without guessing business behavior? | YES |

**READY_FOR_SYSTEM_DESIGN = YES**, with the explicit caveat that the `BLOCKING` items in Section 20 (CQ-001, CQ-002, CQ-003/CQ-013, CQ-016, and the conditional Gap-EMERGENCY-OVERRIDE) must be resolved before the specific requirements they affect (guest checkout eligibility, the exact cancellation cutoff and full transition validity set, quantitative resilience/timeout/lock parameters, and the shipping-fee calculation rule) are treated as final for code generation. All other requirements in this document are complete and implementable as specified.