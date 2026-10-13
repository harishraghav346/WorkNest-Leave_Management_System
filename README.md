# WorkNest 🏢

### Unified Employee Leave Management System

WorkNest is a full-stack web application designed to simplify employee leave management, streamline approval workflows, and centralize leave administration through a unified platform.

Built using **React.js, Spring Boot, and H2 Database**, WorkNest aims to make leave-related operations more organized, transparent, and efficient.

---

## 📌 Table of Contents

* [Overview](#-overview)
* [Objectives](#-objectives)
* [Features](#-features)
* [Technology Stack](#️-technology-stack)
* [Project Structure](#-project-structure)
* [Prerequisites](#-prerequisites)
* [Installation and Setup](#-installation-and-setup)
* [Database Configuration](#️-database-configuration)
* [Application Workflow](#-application-workflow)
* [Security Considerations](#-security-considerations)
* [Testing](#-testing)
* [Future Enhancements](#-future-enhancements)
* [Contributing](#-contributing)
* [License](#-license)
* [Author](#-author)

---

## 📖 Overview

Managing employee leave manually can lead to administrative overhead, communication delays, and difficulty tracking requests.

WorkNest aims to address these challenges by providing a centralized application for handling employee leave requests and approval processes.

The application uses a React frontend to deliver the user interface and a Spring Boot backend to implement server-side functionality, with H2 serving as the database according to the project's current configuration.

## 🎯 Objectives

* Simplify employee leave application and approval processes.
* Centralize leave request information.
* Improve visibility into leave request statuses.
* Reduce manual effort in leave administration.
* Provide a maintainable foundation for future HR management capabilities.

## ✨ Features

The application is intended to support the following capabilities, subject to the current implementation:

* **Leave Request Management:** Submit and manage employee leave requests.
* **Approval Workflow:** Support the review and approval or rejection of leave requests.
* **Request Status Tracking:** Track the status of leave applications.
* **Centralized Management:** Maintain leave-related information through a unified application.
* **Extensible Architecture:** Support future improvements to workforce and HR management.

> **Note:** The exact functionality depends on the features implemented in the current source code.

## 🛠️ Technology Stack

| Component                | Technology        |
| ------------------------ | ----------------- |
| Frontend                 | React.js          |
| Backend                  | Java, Spring Boot |
| Database                 | H2 Database       |
| Frontend Package Manager | npm               |
| Backend Build Tool       | Maven             |
| Version Control          | Git               |
| Repository Hosting       | GitHub            |

## 📂 Project Structure

```text
HR-Leave-Tracker/
├── reactapp/
│   ├── public/
│   ├── src/
│   ├── package.json
│   └── ...
├── springapp/
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   ├── pom.xml
│   └── ...
├── .gitignore
└── README.md
```

*The structure shown above is illustrative. Refer to the repository for the actual directory structure.*

## ⚙️ Prerequisites

Before running WorkNest, ensure the following software is installed:

* **Java JDK:** A version compatible with the backend configuration.
* **Node.js and npm:** For frontend dependency management and development.
* **Maven:** For building and running the backend, unless the project includes a Maven Wrapper.
* **Git:** For version control.

Verify your installations:

```bash
java -version
node -v
npm -v
git --version
```

## 🚀 Installation and Setup

### 1. Clone the Repository

```bash
git clone https://github.com/harishraghav346/HR-Leave-Tracker.git
```

Navigate to the project directory:

```bash
cd HR-Leave-Tracker
```

### 2. Configure and Start the Backend

Navigate to the backend directory:

```bash
cd springapp
```

**On Windows, if the project includes Maven Wrapper:**

```powershell
.\mvnw.cmd spring-boot:run
```

**Alternatively, if Maven is installed:**

```bash
mvn spring-boot:run
```

The backend will start on the port configured in the Spring Boot application properties.

### 3. Start the Frontend

Open a separate terminal from the project root and navigate to the frontend directory:

```bash
cd reactapp
```

Install the required dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

If the project uses a different development command, refer to the `scripts` section in `reactapp/package.json`.

Open the local URL displayed in the terminal.

### 4. Connect the Frontend and Backend

Ensure that the frontend API configuration points to the correct backend address and port.

If the frontend and backend run on different origins, verify that the backend's CORS configuration permits requests from the frontend development server.

## 🗄️ Database Configuration

WorkNest uses H2 Database according to the project's current configuration.

The database settings are typically located in:

```text
springapp/src/main/resources/application.properties
```

Review the configuration to determine:

* Database connection URL.
* In-memory or file-based storage.
* Schema initialization strategy.
* Database console configuration, if enabled.

Database settings may differ between development and production environments.

**Important:** Do not commit database files containing sensitive information, production credentials, API keys, or private configuration to a public repository.

## 🔄 Application Workflow

The general leave management workflow is:

1. An employee submits a leave request.
2. The backend validates and processes the request.
3. The designated approver reviews the application.
4. The request is approved or rejected according to the configured workflow.
5. The application reflects the updated request status.

The actual approval rules, user roles, and leave balance calculations depend on the implemented business logic.

## 🔐 Security Considerations

Security is an important consideration for applications that manage employee information.

Recommended practices include:

* Validate requests on the backend.
* Enforce authorization for protected API endpoints.
* Protect authentication credentials and API secrets.
* Avoid exposing sensitive employee information.
* Store passwords securely if authentication is implemented.
* Keep private configuration and environment files out of version control.
* Use appropriate access controls for database and administrative functionality.

Frontend access restrictions alone do not provide sufficient backend authorization.

## 🧪 Testing

Run the tests available in the project before deploying changes.

For a Maven-based backend:

```bash
cd springapp
mvn test
```

If Maven Wrapper is available, use the corresponding wrapper command instead.

For frontend testing, check the scripts defined in `reactapp/package.json`.

## 🔮 Future Enhancements

Potential enhancements include:

* Employee and administrator dashboards.
* Leave balance tracking and validation.
* Configurable approval workflows.
* Leave history and reporting.
* Notifications for leave request updates.
* HR analytics and visual reports.
* Audit logging and enhanced access controls.
* Responsive user interface improvements.
* Integration with other HR systems.

These items represent possible future development, not necessarily existing functionality.

## 🤝 Contributing

Contributions, suggestions, and bug reports are welcome.

To contribute:

1. Fork the repository.
2. Create a feature branch.
3. Implement your changes.
4. Test your changes.
5. Commit with a descriptive message.
6. Submit a pull request explaining your improvements.

For significant changes, consider opening an issue before beginning implementation.

## 📄 License

This project is proposed to be licensed under the **MIT License**.

Copyright (c) 2026 Harish Raghavendraa

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

**Note:** The MIT License applies only if you choose to release your original contributions under these terms. Add a separate `LICENSE` file containing the license text to make the repository's licensing explicit. Third-party dependencies remain subject to their respective licenses.

## 👨‍💻 Author

**Harish Raghavendraa**

GitHub: [@harishraghav346](https://github.com/harishraghav346)

Project Repository: [WorkNest - HR Leave Tracker](https://github.com/harishraghav346/HR-Leave-Tracker)

---

<div align="center">

**WorkNest** — Simplifying employee leave management.

</div>
