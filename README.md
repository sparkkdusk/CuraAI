# CuraAI
### AI-Powered Personal Health Assistant

CuraAI is an AI-powered personal health assistant that brings together conversational AI, prescription recognition, nutrition analysis, and computer vision-based exercise tracking in a unified platform.

Built with Java Spring Boot, PostgreSQL, and modern AI technologies, CuraAI helps users manage personal health information, track daily activities, and gain insights from their medication, nutrition, and exercise records.

## Features

### AI Health Assistant
- Conversational health assistance primarily powered by Groq.
- Google Gemini integration for multimodal reasoning.
- Voice API integration for voice-related functionality.
- Basic Retrieval-Augmented Generation (RAG) for retrieving relevant personal health records.
- Answers questions about medication, nutrition, and exercise history using stored user data.

### Prescription Recognition
- Extracts information from prescription images using OCR.
- Processes and organizes prescription information.
- Maintains medication records and history.
- Provides AI-assisted insights based on stored medication information.

### Nutrition Analysis
- Analyzes food images using multimodal AI.
- Estimates calorie content and nutritional information.
- Supports daily and weekly nutrition tracking.
- Provides insights into food consumption.

### Computer Vision-Based Exercise Tracking
- Real-time pose detection using MediaPipe and OpenCV.
- Tracks body movements through computer vision.
- Supports squat, push-up, and shoulder exercise monitoring.

## Technology Stack

| Category | Technologies |
|---|---|
| Backend | Java, Spring Boot |
| Frontend | Thymeleaf, Tailwind CSS, JavaScript |
| Database | PostgreSQL |
| Primary AI Integration | Groq |
| Multimodal AI | Google Gemini |
| Voice Integration | Voice API |
| Computer Vision | MediaPipe, OpenCV |
| OCR | Prescription Text Recognition |
| API Architecture | REST APIs |
| Containerization | Docker, Docker Compose |
| Version Control | Git, GitHub |

## AI and Data Integration

CuraAI combines multiple AI capabilities with personal health data to provide more relevant and context-aware interactions.

Groq serves as the primary provider for conversational AI, while Google Gemini supports multimodal reasoning and image-based analysis. Voice API integration provides voice-related functionality.

The platform also implements basic Retrieval-Augmented Generation (RAG), retrieving relevant information from stored health records to answer natural-language queries such as:

- What did I eat today?
- What medications do I currently have?
- What exercises did I perform today?

## Getting Started

### Prerequisites

- Git
- Docker Desktop
- Groq API key
- Google Gemini API key
- Voice API credentials (if required by the voice integration)

### Installation

**1. Clone the repository**

```bash
git clone https://github.com/sparkkdusk/CuraAI.git
cd CuraAI
```

**2. Configure environment variables**

Create a `.env` file in the location expected by your Docker Compose configuration.

Example:

```env
GROQ_API_KEY=your_groq_api_key
GEMINI_API_KEY=your_gemini_api_key

DB_URL=your_postgresql_connection_url
DB_USERNAME=your_database_username
DB_PASSWORD=your_database_password

PORT=8080
```

Add the required Voice API credentials using the variable names defined in the application configuration.

**3. Build and run with Docker**

```bash
docker compose up --build
```

To run the containers in the background:

```bash
docker compose up --build -d
```

**4. Access the application**

```text
http://localhost:8080
```

The application port may vary depending on the Docker configuration.

**5. Stop the application**

```bash
docker compose down
```

To remove the database volume as well:

```bash
docker compose down -v
```

Note that this also deletes persisted database data.

## Security

- API credentials are managed through environment variables.
- Sensitive configuration files are excluded from version control.
- Database credentials are not stored in source code.
- Personal health information should be handled securely.

## Future Improvements

- More advanced RAG pipelines and contextual retrieval.
- Persistent conversational history and memory.
- Expanded personal health analytics.
- Improved prescription recognition and medication insights.
- Enhanced exercise recognition and personalized recommendations.

## Project Background

CuraAI was developed as an AI-focused project exploring practical applications of artificial intelligence and computer vision in personal health management.

The project brings together multiple AI capabilities to make personal health information more accessible, understandable, and useful in everyday life.

---

**CuraAI — Intelligent assistance for everyday health management.**
