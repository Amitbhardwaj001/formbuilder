<!-- PROJECT TITLE -->

<h1 align="center">📄 Form Builder Application</h1>

<p align="center">
  🚀 Build • Share • Analyze Forms with Ease
</p>

<p align="center">
  <a href="#"><img src="https://img.shields.io/badge/Backend-SpringBoot-brightgreen"/></a>
  <a href="#"><img src="https://img.shields.io/badge/Frontend-TailwindCSS-blue"/></a>
  <a href="#"><img src="https://img.shields.io/badge/Database-MySQL-orange"/></a>
  <a href="#"><img src="https://img.shields.io/badge/API-REST-yellow"/></a>
</p>

---

## 🌐 Live Demo

👉  COMING SOON...

---

## 📌 Overview

A full-stack **Form Builder** platform that enables users to create customizable forms, share them via links, and analyze responses in real time.

Designed with **scalability, clean architecture, and real-world deployment practices**.

---

## ✨ Key Features

* 📝 Dynamic Form Creation (MCQ, text, file upload)
* 🔗 Shareable Public Form Links
* 📊 Analytics Dashboard for Responses
* 🔐 Authentication System (Login/Signup)
* ⚡ Real-time Rendering
* 🧠 AI Integration (Gemini API)
* 📁 File Upload Support

---

## 🧠 System Architecture

```text
Frontend (HTML/CSS/JS + Tailwind)
        ↓
REST API (Spring Boot)
        ↓
Service Layer (Business Logic)
        ↓
Repository Layer (JPA/Hibernate)
        ↓
MySQL Database
```

---

## 🛠️ Tech Stack

| Layer    | Technology                          |
| -------- | ----------------------------------- |
| Backend  | Java, Spring Boot, Hibernate        |
| Frontend | HTML, CSS, JavaScript, Tailwind CSS |
| Database | MySQL                               |
| Tools    | Git, GitHub, Postman                |
| AI       | Gemini API                          |

---

## ⚙️ Setup Guide

### 1️⃣ Clone Repository

```bash
git clone https://github.com/your-username/formbuilder.git
cd formbuilder
```

### 2️⃣ Configure Environment Variables

```bash
DB_URL=your_database_url
DB_USERNAME=your_username
DB_PASSWORD=your_password
GEMINI_API_KEY=your_key
```

### 3️⃣ Run Application

```bash
./mvnw spring-boot:run
```

---

## 📌 API Reference

| Method | Endpoint    | Description     |
| ------ | ----------- | --------------- |
| GET    | /forms      | Fetch all forms |
| POST   | /forms      | Create new form |
| GET    | /forms/{id} | Fetch form      |
| POST   | /responses  | Submit response |
| GET    | /analytics  | View analytics  |

---

## 📸 Screenshots


> 

---

## 🚀 Deployment

* Cloud-ready (Render / Railway / AWS)
* Uses environment variables for secure config
* Supports external MySQL databases

---

## 🔐 Environment Variables

| Variable       | Purpose             |
| -------------- | ------------------- |
| DB_URL         | Database connection |
| DB_USERNAME    | DB user             |
| DB_PASSWORD    | DB password         |
| GEMINI_API_KEY | AI integration      |

---

## 📊 Why This Project Stands Out

✔ Real-world full-stack architecture
✔ Production-ready configuration
✔ Clean REST API design
✔ Includes authentication + analytics + AI
✔ Deployment-ready

---

## 👨‍💻 Author

**Amit Bhardwaj**

* GitHub: https://github.com/Amitbhardwaj001

---

## ⭐ Show Your Support

If you found this project useful:

👉 Star the repo
👉 Share it
👉 Use it

---

<p align="center">
  🔥 Built with passion & real-world engineering mindset
</p>
