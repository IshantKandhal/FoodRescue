# Food Rescue Plan

A console-based Java application that connects food donors, NGOs, and volunteers to reduce food wastage. Donors list surplus food, NGOs request what they need, and the system automatically matches donations and assigns volunteers for pickup and delivery.

---

## Problem Statement

Restaurants, hotels, hostels, canteens, weddings, and other events often have excess food that is still fit for consumption, while many people and organizations need food assistance. This project builds a bridge between the two — reducing wastage and helping surplus food reach people who need it.

---

## How It Works

1. **Donor** lists a food donation — food name, quantity, and expiry date.
2. **NGO** requests a food item and quantity it needs.
3. The system searches all matching donations and picks the one **closest to expiring** first — minimizing wastage by using soon-to-expire food before anything else.
4. Once a donation is matched, the system **automatically assigns a free volunteer** to the delivery — no manual lookup needed.
5. The **volunteer** can view their assigned deliveries and mark them complete once done.
6. An NGO can **cancel a pending request**, and the reserved food quantity is automatically restored to the original donation.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java (console-based) |
| Database | MySQL |
| Connectivity | JDBC (manual, no ORM/framework) |
| Data integrity | Transactions (commit/rollback) for multi-step operations |

---

## File Breakdown

### `MainMenu.java`
Entry point of the application. Displays the main menu (Donor / NGO / Volunteer / Exit), creates one shared `Scanner` and passes it to every other class, and routes the user into the correct module.

### `DBconnection.java`
Single utility class responsible for opening a JDBC connection to MySQL. Every other class calls `DBconnection.getConnection()` instead of writing its own connection logic.

### `Donor.java`
- Add a food donation (food name, quantity, expiry date)
- View all donations submitted by that donor
- Validates input (non-empty fields, quantity > 0)

### `NGO.java`
- Request a food item and quantity
- **Matching logic:** finds all available, non-expired donations for that food item, sorted by expiry date (soonest first), and checks if the available quantity covers the request
- On a successful match: creates the request, records the allocation (which donation fulfilled it and how much), reduces the donation's remaining quantity, and **automatically triggers volunteer assignment** — all inside a single database transaction
- View my requests / view allocation details / view delivery details
- Cancel a pending request — restores the donation's quantity back so it isn't lost from the system

### `Volunteer.java`
- View deliveries assigned to the logged-in volunteer
- Mark a delivery as complete (frees up the volunteer for the next assignment)
- `assignVolunteer()` — the core assignment logic: prefers the currently logged-in volunteer if they're free, otherwise picks any other available volunteer. Called automatically by `NGO.java` right after a successful match.

### `InvalidQuantityException.java`
Custom checked exception thrown when a quantity entered by the user is zero or negative.

---

## Database Schema

| Table | Purpose |
|---|---|
| `donations` | Food listed by donors — name, quantity, expiry date, status |
| `requests` | Food requested by NGOs and its fulfillment status |
| `allocations` | Records which donation fulfilled which request, and how much quantity was taken |
| `deliveries` | Links a request to the volunteer assigned to deliver it |
| `volunteers` | Volunteer directory with availability status (`Free` / `Busy`) |

---

## Key Design Decisions

- **Expiry-priority matching** — when multiple donations match a request, the one expiring soonest is used first, directly reducing food wastage rather than just doing a plain lookup.
- **Automatic volunteer assignment** — no one needs to know a volunteer's name to trigger an assignment; it happens the moment a request is successfully matched.
- **Transactional writes** — request creation, allocation, and quantity updates happen together; if any step fails, everything rolls back so the database never ends up in a half-updated state.
- **Single shared `Scanner`** — one `Scanner` instance is created in `MainMenu` and passed into every class via its constructor, avoiding input-buffering conflicts that occur when multiple classes each open their own `Scanner` on `System.in`.

---

## Setup

1. Run the schema script in MySQL to create the database and tables.
2. Update the credentials in `DBconnection.java` with your own MySQL username/password.
3. Download the MySQL Connector/J `.jar` and add it to your project's classpath / `Referenced Libraries`.
4. Compile and run `MainMenu.java`.

---

## Known Limitations / Future Enhancements

- A request can currently only be fulfilled by a **single** donation — if no single donation has enough quantity, the request is rejected rather than combining multiple donations to fulfill it.
- No location-based matching between donors and NGOs yet.
- No login/authentication — the app is menu-driven by name entry only.

---

## Team

| Module | Responsibility |
|---|---|
| Database design + Main Menu | Integration and overall flow |
| Donor module | Food donation intake |
| NGO module | Matching, allocation, and request logic |
| Volunteer module | Delivery assignment and completion |
