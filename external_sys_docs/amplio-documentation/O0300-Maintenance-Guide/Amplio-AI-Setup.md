# References

| Reference | Title | Author |
|-----------|-------|--------|
| [Easley AI reference application] | Easley AI reference application | Netcompany |
| [OpenAI – Create chat completions endpoint specification] | OpenAI - Create chat completion endpoint specification | OpenAI |

# Introduction

This user guide provides instructions for installing, configuring, and using AmplioAI—a centralized solution designed to accelerate the integration of AI capabilities into Amplio projects. It covers the setup of the underlying Easley AI Gateway, the integration of the Amplio AI Java Library, and the usage of the Virtual Assistant feature.

While AmplioAI can connect directly to any AI model provider, we strongly recommend integrating with the Easley AI ecosystem to ensure a seamless and fully optimized experience. Therefore, in this user guide, we will mainly focus on using Easley AI as the main gateway to any AI provider.

## Intended Audience

* Developers Integrating AI features (Chat, RAG) into Amplio Java projects.
* Architects and managers are seeking an understanding of how the AmplioAI connects with Easley AI services.
* End users who are using and configuring the Virtual Assistant feature.

## Scope

* Overview of AmplioAI role and capabilities.
* Step-by-step setup and installation instructions.
* Usage guidance with practical examples and typical use cases.

# Terminology

| Term | Description |
|------|-------------|
| **Chat Completions** | OpenAI-compatible API context, chat completions refer to the API endpoint and data format used to generate responses from chat-based large language models (LLMs) |
| **Embeddings** | Numeric vector representations of text, used for semantic search, similarity, and RAG-based retrieval. |
| **RAG** | Retrieval-Augmented Generation. An AI technique that grounds model responses in project-specific documents for more accurate answers. |
| **Virtual Assistant** | An AI-powered chat interface within Amplio that uses background knowledge to answer user queries. |
| **Streaming (SSE)** | Server-Sent Events; a mechanism for real-time, incremental delivery of AI responses over HTTP. |

# Getting started

## Prerequisites

To set up and use the AI Gateway, ensure you meet the following requirements:

* **Docker Runtime Environment:** The AI Gateway is distributed as a Docker image. You need Docker runtime (such as Docker Desktop, Rancher Desktop, or Podman) installed on your machine to run the service.
* **LLM Provider API Key:** You must provide an API key from a supported Large Language Model (LLM) provider (e.g., OpenAI, Azure OpenAI) for the AI Gateway to process AI requests.
* **Important:** Default distribution of EASLEY AI Gateway requires subscription and access to the `text-embedding-3-small` embedding model exposed via Azure OpenAI. The AI Gateway depends on this model to be fully functional—without it, key features like embeddings will not work, and errors will occur. Ensure your API key has the necessary permissions before setting up.
* **Authorization for JFrog Artifactory:** The AI Gateway Docker images are hosted in Netcompany's JFrog Artifactory, within the `ncdotnet-dev-docker` repository.

**Requesting Access:**
Follow the steps below to request access to the repository. Create a new case with the following fields:

![Create new case in ITS](./.attachments/Amplio-AI-Setup/Create-new-case-in-ITS.png)

> **Figure 1: Create new case in ITS**
Request approval from manager for the case. For production ready setup it is recommend to use service accounts for interactions with JFrog Artifactory.

**Authorization for EASLEY AI:**
In order to pull the latest code and get the required setup files, you also need to have access to AI Gateway repository of [Easley AI Reference application](#references). Please request access to that repository as well.

## Installation

This section shows the steps to setup and runs AI Gateway.

### Clone Easley AI Reference application

Go to [Easley AI Reference application](#references) and clone the repository.

### Configure appsettings.json

Navigate to: `.\infrastructure\release\gateway\`. There are three `appsettings.json` file, where each of them matches with its corresponding client of AI Gateway. Open all the `appsettings.json` file in a text editor and complete the following placeholders:

**Database connection string (required)**
This is required for all three appsettings.

```json
"ConnectionStrings": {
    "EasleyAIDatabaseContext": "#{EasleyAIDatabaseConnectionString};Command Timeout=300;",
    "HangfireDbConnection": "#{EasleyAIDatabaseConnectionString}"
},
```

**Example for local setup:**

```json
"EasleyAIDatabaseContext": "Host=postgresql;Port=5432;Database=EasleyAI;Username=postgres;Password=postgres;Command Timeout=300;",
"HangfireDbConnection": "Host=postgresql;Port=5432;Database=EasleyAI;Username=postgres;Password=postgres;"
```

**AI Model Provider Connection**
Regarding setting up the connection between EASLEY and AI Model provider, find the following section and fill in the placeholder. We will use Azure OpenAI as an example, and this is highly recommended to be used since we can easily host the model in EU region, which will comply with EU AI Act.

```json
{
  "AzureOpenAiConfigurations": [
    {
      "ModelName": "gpt4-32k",
      "Configurtions": [
        {
          "ApiHostName": "https://its-ctx-test01-aea-ai001.openai.azure.com/",
          "ApiKey": "API_KEY_GOES_HERE",
          "DeploymentOrModelName": "gpt432k"
        },
        {
          "ApiHostName": "https://its-ctx-test01-cea-ai001.openai.azure.com/",
          "ApiKey": "API_KEY_GOES_HERE",
          "DeploymentOrModelName": "gpt432k"
        },
        {
          "ApiHostName": "https://its-ctx-test01-eus2-ai001.openai.azure.com/",
          "ApiKey": "API_KEY_GOES_HERE",
          "DeploymentOrModelName": "gpt432k"
        }
      ]
    },
    {
      "ModelName": "gpt4",
      "Configurtions": [
        {
          "ApiHostName": "https://its-ctx-test01-aea-ai001.openai.azure.com/",
          "ApiKey": "API_KEY_GOES_HERE",
          "DeploymentOrModelName": "gpt4"
        },
        {
          "ApiHostName": "https://its-ctx-test01-eus2-ai001.openai.azure.com/",
          "ApiKey": "API_KEY_GOES_HERE",
          "DeploymentOrModelName": "gpt4"
        }
      ]
    },
    {
      "ModelName": "gpt3.5-turbo",
      "Configurtions": [
        {
          "ApiHostName": "https://its-ctx-test01-eus2-ai001.openai.azure.com/",
          "ApiKey": "API_KEY_GOES_HERE",
          "DeploymentOrModelName": "gpt35turbo"
        }
      ]
    }
  ]
}
```

>
> **Important:** The credentials and configuration for the `text-embedding-3-small` model must be provided, as this is required to enable embedding features.
>

**Hangfire Dashboard Setup:**
For Hangfire dashboard for background jobs, note that the local setup starts the Keycloak Identity Provider (IdP) in development mode, which operates only over the HTTP protocol. This configuration is intended solely for local development and is not suitable for production use.

* In this setup, the Hangfire dashboard authentication must be configured with `UseHttpsRedirectUri` set to `false`, matching the HTTP-based communication of the local Docker container.
* For production or any environment requiring HTTPS, set `UseHttpsRedirectUri` to `true` and ensure a production-ready configuration is in place.

## Run AI Gateway

Follow the following steps to get the AI Gateway up and running.

### Login to Netcompany JFrog Artifactory

Navigate to `./scripts` and run `./ai-gateway-docker-login` script and provide credentials information when prompted.

### Find the image tag on JFrog

1. Access `https://repo.netcompany.com/ui/login/` with your credentials.
2. Go to Artifacts, search for `ncdotnet-dev-docker`, find `nceasley-easley` and open the Gateway to find the latest image tag.
3. As in the following image, the latest image tag is: `20251110.477439`.

>
![Image tag on JFrog Artifactory](./.attachments/Amplio-AI-Setup/Find-the-image-tag-on-Jrog.png)
>
> **Figure 2:Image tag on JFrog Artifactory**
> 
>

This tag value is correlated with the *Azure DevOps pipeline Build and push Easley AI demo environment*, which is produced during the build (and optionally deployment) process for Easley. This allows you to identify exactly the newest image tag Id.

>
![Image tag on Azure DevOps](./.attachments/Amplio-AI-Setup/Newst-Image-tag-on-Azure-Devops-pipeline-Build.png)
>
> **Figure 3:Image tag on Azure DevOps**
>

### Run AI Gateway and its supporting images

1. Open `docker-compose.yml` and replace all `<IMAGE_TAG>` with the above tag that we get in step 3.3.2.
2. Then open PowerShell, go to folder `infrastructure/release/gateway` and run `./deploy.ps1`.
3. All the images must be ready to be used after the setup is complete.

### Access Hangfire dashboard

To check the running jobs from Hangfire, access url: `http://localhost:7236/jobs/`. This page is hidden behind a Keycloak authentication page, using the following credentials to login:

| Username | Password |
|----------|----------|
| developer | 42_NewCoolNetcompanyPassword |

# Using Amplio AI Library (Developer Guide)

This section guides developers on integrating the Amplio AI library into Java projects. It is organized by capability, providing the necessary code to implement Chat, RAG, and Tooling features.

## Configuration

### Application Properties

Add the following configurations to your project's `application.properties` to connect to the Gateway:

```properties
# OpenAI / AI Gateway Configuration
nc.amplio.libraries.ai.openai.base-url=http://localhost:8123
nc.amplio.libraries.ai.openai.api-key=[YOUR_API_KEY]
nc.amplio.libraries.ai.openai.embeddings.options.model=text-embedding-3-small
nc.amplio.libraries.ai.openai.chat.options.temperature=0.7

# Chat Memory
nc.amplio.libraries.ai.chat_memory.max_messages=20

# RAG Configuration
nc.amplio.libraries.ai.rag.documents-endpoint=http://localhost:8123/
nc.amplio.libraries.ai.rag.polling-delay-seconds=3
```

### Database Patches

The library requires specific database tables (`AI_DOCUMENT`, `AI_PROMPT_HISTORY`, `AI_FEEDBACK`) and security roles. Ensure the Flyway migrations provided in detailed design are applied.

## Usage Examples

### Chat Completion (Generating Responses)

The `ChatService` is your primary interface for interacting with LLMs. It handles the complexity of constructing requests and parsing responses.

**Task: Generate a Simple Response**
For standard interactions where you need a complete answer at once (Synchronous), use `getCompletion`. You should use the `PromptBuilderFactory` to structure your inputs, which ensures your prompts follow best practices (separating Identity from User Messages).

```java
// 1. Construct the Prompt
Prompt prompt = promptBuilderFactory.create()
    .setIdentity("You are a helpful assistant")
    .addUserMessage("Hello, how are you?")
    .build();

// 2. Build the Request
ChatCompletionRequest request = ChatCompletionRequest.builder()
    .chatOptions(new ChatOptions(0.7, "gpt-4"))
    .prompt(prompt)
    .conversationId("conv-123") // Optional: For tracking history
    .build();

// 3. Execute
ChatCompletionResponse<Object> response = chatService.getCompletion(request);
```

**Task: Stream a Response (Real-Time)**
For chat interfaces where low latency is critical, use `streamCompletion`. This returns chunks of text as they are generated, improving the user experience.

```java
ChatCompletionStreamRequest streamRequest = ChatCompletionStreamRequest.builder()
    .chatOptions(new ChatOptions(0.7, "gpt-4"))
    .promptBuilder(promptBuilderFactory.create().addUserMessage("Tell me a story"))
    .build();

// Returns a reactive Flux stream
chatService.streamCompletion(streamRequest)
    .subscribe(chunk -> System.out.print(chunk.getContentChunk()));
```

**Task: Enforce Structured Output**
If you need the AI to return data (e.g., JSON) instead of free text, define a Java class and pass it to the `responseType` method. The library will automatically map the AI's response to your class.

```java
ChatCompletionRequest request = ChatCompletionRequest.builder()
    .responseType(MyCustomPojo.class) // The target class
    .build();
```

### RAG: Document Management & Search

The `AiDocumentService` allows you to manage "Knowledge"—documents that the AI can read and reference to answer questions.

**Upload a Knowledge Document:**
Use `uploadDocumentAsync` to send a file (PDF, DOCX, TXT) to the Easley AI Gateway. The Gateway will process, chunk, and embed the document in the background.

```java
byte[] content = Files.readAllBytes(Path.of("manual.pdf"));

UploadDocumentResultAsync result = aiDocumentService.uploadDocumentAsync(
    "doc-id-001",                   // Unique Document ID
    AiFeatureTypes.VIRTUAL_ASSISTANT, // Feature context
    content,
    "manual.pdf",
    "application/pdf"
);
```

**Search Documents (Semantic Search):**
To find relevant information, use `semanticSearch`. This retrieves the most relevant text chunks from your uploaded documents based on the meaning of the user's query.

```java
List<SemanticSearchResponseItem> results = aiDocumentService.semanticSearch(
    List.of("doc-id-001"),          // Documents to search
    "How do I reset the password?", // User Query
    0.7,                            // Certainty threshold (0.0-1.0)
    5                               // Max chunks to retrieve
);
```

### Tooling & Auditing

The library provides helper services to support auditing and history management.

**Submit User Feedback:**
Use `AiFeedbackService` to store user ratings (1-5) and comments. This is essential for auditing the quality of AI responses.

```java
aiFeedbackService.submitFeedback("Very helpful!", 5);
```

**Embed Text Directly:**
If you need raw vector embeddings for your own comparison logic (without storing data), use the `EmbeddingService`.

```java
EmbeddingResponse response = embeddingService.getEmbeddings(
    new EmbeddingRequest(List.of("Text to embed"))
);
```

# Virtual Assistant Feature (User & Admin Guide)

The Virtual Assistant is an integrated AI tool designed to assist users by answering questions based on specific background knowledge. It operates as a read-only assistant, meaning it can summarize data and explain concepts but cannot execute system actions directly.

## Capabilities

* The assistant is accessible via a floating circle icon in the corner of the application.
* Clicking this icon expands the chat panel, which persists as you navigate between different pages of the application.
* Inside the chat panel, you have access to a **Chat Options Menu** (indicated by three dots `⋮`). This menu provides context-specific actions:
  * **Create page summary:** The assistant fetches the data currently displayed on your screen (such as an entity or unit page) and generates a summary. This is useful for quickly understanding the status of an entity without reading every field manually.
  * **Start new chat:** This clears the current conversation context, ensuring that previous topics do not confuse the AI during a new query.
  * **Give feedback:** This opens a form where you can rate the AI's helpfulness and provide text comments, which are stored for system administrators to review.

**Document Management:**
The assistant can utilize two types of documents for context.

* **Permanent documents:** uploaded by administrators and are always available to the assistant.
* **Session documents:** temporary files you upload directly within the chat interface using the document icon.
* These session documents are scoped only to your current conversation and are automatically deleted by a background cleanup job after a set retention period (typically 24 hours).
* You can manage these active documents by toggling them on or off in the document menu, controlling exactly which sources the AI uses to answer your questions.

## User Interface Guide

* **Access:** Click the Virtual Assistant icon in the corner of the Amplio UI.
* **Chat:** Type your query in the input field. The assistant will respond based on the "Knowledge & Data" available to it.
* **Document Management:**
  * Click the **Document icon** in the chat input area to upload new documents (PDF, DOCX, TXT).
  * Select/Deselect specific documents to include them in the current conversation's context.
* **Feedback:** Click the **Options menu (`⋮`)** > **Give feedback** to rate the helpfulness of a response.

## Configuration (System Parameters)

AmplioAI allow users to configure AI behavior based on their feature types. This can help to configure the style of the response from AI can be varied depending on different situations, for example, the Virtual Assistant to be professional, and another AI feature which can generate text to be helpful.

Administrators can customize the Virtual Assistant's behavior using **System Parameters** (type: `ai_features`). This configuration allows you to modify the AI's persona and technical settings without deploying new code.

>
![System parameter type for AI Configuration](./.attachments/Amplio-AI-Setup/System-parameter-type-for-AI-Configuration.png)
>
> *[Figure 1: System parameter type for AI Configuration]*
>

For the Virtual Assistant, there many configurations to be modified, with the key configurations shown in the interface.

>
![Virtual assistant configuration](./.attachments/Amplio-AI-Setup/Virtual-Assistant-Configuration.png)
>
> *[Figure 2: Virtual assistant configuration]*
>

**Key Parameters:**

* **IDENTITY:** Defines who the AI is (e.g., "You are the Amplio Virtual Assistant...").
* **STYLE_INSTRUCTION:** Guidelines for formatting and refusal (e.g., "Use bullet points," "Do not judge the topic").
* **PERSONALITY:** Traits like "Professional, objective, helpful".
* **MODEL:** Specifies the underlying LLM (e.g., `gpt-4-o-mini`).
* **TEMPERATURE:** Controls creativity on a scale of 0.0 to 2.0. A default of 0.7 is used to balance factual accuracy with natural phrasing.
* **SEMANTIC SEARCH CERTAINTY:** Defines the minimum similarity score (0.0 to 1.0) required for a document chunk to be considered relevant. A lower value like 0.1 ensures a broader search retrieval.

## Compliance

* **Transparency:** The UI explicitly indicates that responses are AI-generated and may be inaccurate.
* **Data Privacy:** The solution is designed (via Azure OpenAI) such that customer data is **not** used to train the public models.

## Language support

The assistant defaults to English but explicitly supports Danish. If a user selects Danish, the system injects specific instructions to force the response into that language. For other languages, the assistant relies on the AI model's native ability to detect and switch languages based on the user's input.