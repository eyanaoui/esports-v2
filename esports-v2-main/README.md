# 🎮 Esports Management Platform

A full-featured **Esports Management System** providing both an **Admin Dashboard** and a **User Interface** for managing teams, tournaments, guides, events, statistics, messaging, and marketplace features.

Designed as a centralized platform for organizing esports ecosystems efficiently.

---

# 🚀 System Architecture

The platform contains **two main interfaces**

### 🛠 Admin Dashboard

Provides full control over platform data and operations

Administrators can manage:

* Teams & team members
* Tournaments & tournament participation
* Games & matches
* Guides, steps, and ratings
* Events
* Users & roles
* Statistics dashboard
* Messaging system
* Forum discussions
* Marketplace products & orders
* Recommendation engine
* Forecasting system

Includes:

✔ Stats analytics
✔ Dark mode support
✔ Admin → User view switch

---

### 👤 User Interface

Users can interact with the esports platform by:

* Browsing teams
* Viewing tournaments & matches
* Reading step-by-step guides
* Rating guides
* Participating in discussions
* Sending messages
* Viewing statistics
* Exploring events
* Receiving recommendations
* Accessing marketplace features

---

# 🧩 Core Modules

The system is composed of:

* Team Management Module
* Tournament Management Module
* Game & Match Module
* Guide System (steps + ratings)
* Event Management Module
* Messaging System
* Forum Module
* Statistics Dashboard
* Marketplace System
* Recommendation Engine
* Forecasting Engine
* User Role Management

---

# 🗄 Database Structure

Main entities include:

```
user
team
team_members
team_stats
tournament
tournament_teams
game
game_match
guide
guide_step
guide_rating
product
order
order_item
product_forecast
product_recommendation
forum_messages
messenger_messages
```

---

# ⚙ Technologies Used

### Backend

* Java
* Spring Boot
* REST API

### Frontend

* JavaFX Interface

### Database

* MySQL

### Build Tool

* Maven

---

# 📦 Installation

Clone repository

```
git clone https://github.com/eyanaoui/e-sports.git
```

Navigate into project

```
cd e-sports
```

Run application

```
mvn spring-boot:run
```

---

# ▶ Running the Application

Start server and open

```
http://localhost:8080
```

Login as:

👨‍💼 Admin → Full dashboard access
👤 User → Standard platform interface

Switch between **Admin View** and **User View** inside the application.

---

# 📊 Platform Features Overview

| Feature            | Admin | User    |
| ------------------ | ----- | ------- |
| Team Management    | ✅     | 👀 View |
| Tournament Control | ✅     | 👀 View |
| Guide Creation     | ✅     | 👀 Read |
| Guide Ratings      | ✅     | ✅       |
| Events             | ✅     | ✅       |
| Messaging          | ✅     | ✅       |
| Forum              | ✅     | ✅       |
| Statistics         | ✅     | 👀 View |
| Marketplace        | ✅     | ✅       |
| Recommendations    | ✅     | ✅       |

---

# 🎯 Project Purpose

This platform was developed as an **educational full-stack esports management system** demonstrating:

* backend architecture design
* database modeling
* multi-role access control
* modular feature integration
* dashboard-driven administration workflows

---

# 👥 Contributors

Add contributors here:

* Your Name
* Team Members

---

# 📄 License

Educational project
