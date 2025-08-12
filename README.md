# Verbum Indexer Demo

A modern web-based control panel for the Verbum file indexing system with beautiful pastel UI and comprehensive accessibility features.

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Gradle (included via wrapper)

### Running the Demo

1. **Clone the repository**
   ```bash
   git clone https://github.com/mkondratek/verbum.git
   cd verbum
   ```

2. **Run the demo application**
   ```bash
   ./gradlew :demo:bootRun
   ```
   
   Or on Windows:
   ```bash
   gradlew.bat :demo:bootRun
   ```

3. **Open your browser**
   Navigate to `http://localhost:8080` (or the port shown in the console)

## ✨ Features

### 🎨 Modern UI Design
- **Pastel color scheme** - Calming, easy-on-the-eyes interface
- **Responsive design** - Works seamlessly on desktop, tablet, and mobile
- **Glass morphism effects** - Modern visual styling with backdrop blur
- **Smooth animations** - Interactive feedback with elegant transitions

### 🔍 Search & Query
- **Real-time search** - Query the file index instantly
- **Search history** - Auto-complete with previous searches
- **Result display** - Clear JSON formatting of search results
- **Keyboard shortcuts** - `Ctrl+K` to focus search, `Enter` to search

### 📁 Path Management
- **File picker integration** - Native OS file/directory selection
- **Absolute path validation** - Enforces proper file system paths
- **Visual path list** - See all indexed paths with metadata
- **Individual removal** - Remove specific paths with dedicated buttons
- **Path history** - Remember frequently used directories

### ⚙️ Indexer Control
- **Start/Stop toggle** - Interactive switches with smooth animations
- **Auto-query mode** - Automatically repeat searches every 2 seconds
- **Real-time status** - Live status log with timestamped events
- **Error handling** - Graceful error recovery with user feedback

### ♿ Accessibility Features
- **WCAG 2.1 AA compliance** - Full screen reader support
- **Keyboard navigation** - Complete functionality without mouse
- **Focus management** - Logical tab order and focus indicators
- **High contrast support** - Automatic adaptation for accessibility needs
- **Reduced motion** - Respects user animation preferences

### 🎹 Keyboard Shortcuts
- **`Ctrl+K`** - Focus search input
- **`Alt+P`** - Focus path input  
- **`Alt+L`** - Focus status log
- **`?`** - Show keyboard shortcuts dialog
- **`Enter`** - Submit forms / Search
- **`Escape`** - Clear inputs / Close dialogs

## 🏗️ Project Structure

```
verbum/
├── demo/                          # Demo web application
│   └── src/main/
│       ├── kotlin/                # Backend Kotlin code
│       └── resources/static/      # Frontend assets
│           ├── index.html         # Main HTML page
│           ├── styles.css         # Pastel theme CSS
│           └── script.js          # Interactive JavaScript
├── lib/                           # Core Verbum library
└── gradle/                        # Gradle configuration
```

## 🎯 API Endpoints

The demo includes these RESTful API endpoints:

- **`POST /api/indexer/start`** - Start the indexing service
- **`POST /api/indexer/stop`** - Stop the indexing service  
- **`POST /api/indexer/add`** - Add a file/directory path to index
- **`POST /api/indexer/remove`** - Remove a path from indexing
- **`GET /api/indexer/query?word={word}`** - Search the index for a word

## 🛠️ Development

### Building
```bash
./gradlew build
```

### Running Tests
```bash
./gradlew test
```

### Code Style
```bash
./gradlew ktlintCheck
./gradlew ktlintFormat
```

## 🎨 UI Customization

The interface uses CSS custom properties for easy theming:

```css
:root {
    --primary-color: #8b9dc9;      /* Soft blue-gray */
    --success-color: #8fbc8f;      /* Muted sage green */
    --warning-color: #ddb896;      /* Warm beige */
    --error-color: #d49a9a;        /* Soft coral */
    --bg-primary: #fdfdf8;         /* Warm cream */
    --border-radius: 12px;         /* Rounded corners */
}
```

## 🌟 Highlights

- **Zero hardcoded paths** - Honest file picker respects browser security
- **Progressive enhancement** - Works with modern File System Access API and falls back gracefully
- **Real form validation** - Comprehensive path validation with visual feedback
- **Notification system** - Toast notifications for all user actions
- **Status logging** - Complete audit trail of all operations
- **Error recovery** - Graceful handling of network and API errors

## 📱 Browser Support

- **Chrome/Edge 86+** - Full features including File System Access API
- **Firefox 90+** - Core functionality with traditional file inputs
- **Safari 14+** - Complete support with accessibility features
- **Mobile browsers** - Responsive design adapts to touch interfaces

## 🔮 Future Features & Roadmap

### 🏗️ Core Library Enhancements (`lib/`)
- **Background Indexing** - Run indexing as backgroundable, cancellable tasks
- **Progress Tracking** - Real-time progress reporting for large directory operations
- **Incremental Updates** - Smart delta indexing for modified files only
- **Concurrent Processing** - Multi-threaded indexing with configurable worker pools
- **Memory Management** - Streaming processing for large files without memory overflow
- **Index Persistence** - Database storage with SQLite/H2 backends
- **File Type Filters** - Configurable inclusion/exclusion patterns
- **Content Extraction** - Plugin system for PDF, Office docs, code files
- **Watch Service Integration** - Real-time file system monitoring
- **Index Optimization** - Automatic index defragmentation and compression
- **Search Ranking** - TF-IDF scoring and relevance algorithms
- **Fuzzy Search** - Approximate string matching and typo tolerance

### 🖥️ Demo Application Features (`demo/`)
- **Real-time Progress Bars** - Visual feedback for long-running index operations
- **Background Task Queue** - Job queue with pause/resume/cancel capabilities
- **WebSocket Integration** - Live updates without polling
- **Indexing Dashboard** - Statistics, performance metrics, and health monitoring
- **Advanced Search UI** - Filters, faceted search, search suggestions
- **File Preview** - In-browser preview of indexed file contents
- **Bulk Operations** - Multi-select and batch path management
- **Import/Export** - Configuration backup and restore
- **User Preferences** - Customizable UI themes and behavior settings
- **Mobile App** - Progressive Web App (PWA) capabilities
- **Admin Panel** - System configuration and maintenance tools

### ⚡ Performance & Scalability
- **Async Operations** - Non-blocking I/O for all file system operations
- **Streaming APIs** - Handle large result sets with pagination
- **Caching Layer** - Redis integration for frequent queries
- **Distributed Indexing** - Multi-node processing for enterprise deployments
- **Resource Throttling** - CPU and I/O usage limits to prevent system overload
- **Health Checks** - Monitoring endpoints for operational readiness
- **Graceful Degradation** - Fallback modes when system resources are constrained

### 🔧 Developer Experience
- **REST API Documentation** - OpenAPI/Swagger specification
- **Client SDKs** - Generated clients for Java, Python, JavaScript
- **Docker Support** - Containerized deployment with Docker Compose
- **Configuration Management** - YAML/JSON configuration with validation
- **Logging Framework** - Structured logging with configurable levels
- **Metrics Collection** - Prometheus/Micrometer integration
- **Integration Tests** - Comprehensive test suite with testcontainers

### 🛡️ Security & Enterprise
- **Authentication** - JWT, OAuth2, and API key support
- **Authorization** - Role-based access control (RBAC)
- **Audit Logging** - Complete audit trail of all operations
- **Rate Limiting** - API throttling and quota management
- **Data Encryption** - At-rest and in-transit encryption options
- **GDPR Compliance** - Data privacy and right-to-be-forgotten features

## 🏛️ Architecture Separation

### Core Library (`lib/`)
**Purpose**: Reusable, high-performance indexing engine
- Pure business logic and algorithms
- No UI dependencies or web frameworks
- Pluggable backends and storage options
- Designed for embedding in various applications
- Comprehensive API for programmatic usage

### Demo Application (`demo/`)
**Purpose**: Reference implementation and user interface
- Web-based control panel and monitoring
- Showcases library capabilities
- User-friendly file management interface
- Real-time status and progress visualization
- Educational tool for library evaluation

This separation allows the core library to be:
- **Embedded** in desktop applications
- **Integrated** into existing enterprise systems
- **Deployed** as microservices or serverless functions
- **Used** in command-line tools and scripts

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests and linting
5. Submit a pull request

## 📄 License

This project is part of the Verbum indexing system.

---

**Built with ❤️ using modern web standards and accessibility best practices.**
