package org.example.cmpe_280_final_project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.cmpe_280_final_project.model.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatGPTService {

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String GPT_API_URL = "https://api.openai.com/v1/chat/completions";

    private final TaskService taskService;

    public ChatGPTService(TaskService taskService) {
        this.taskService = taskService;
    }

    // Handle user prompt and process task-related actions
    public String handleUserPrompt(String userPrompt) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> requestBody = new HashMap<>();

        // Instructions for GPT to respond in a structured format
        String systemInstructions = """
        You are an assistant managing tasks. Respond in this format:
        - For adding tasks: {"action": "add", "title": "task title", "description": "task description", "priority": "High/Medium/Low", "dueDate": "YYYY-MM-DDTHH:mm"}
        - For deleting tasks: {"action": "delete", "taskId": "task ID"}
        If the query is unrelated to tasks, simply respond with "No task action recognized."
        """;

        // Combine instructions with the user's prompt
        requestBody.put("model", "gpt-4");
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemInstructions),
                Map.of("role", "user", "content", userPrompt)
        ));
        requestBody.put("max_tokens", 300);
        requestBody.put("temperature", 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(GPT_API_URL, HttpMethod.POST, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("choices")) {
                // Retrieve the 'choices' field as a list of maps
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");

                if (!choices.isEmpty()) {
                    // Extract the first choice
                    Map<String, Object> firstChoice = choices.get(0);

                    if (firstChoice.containsKey("message")) {
                        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                        String content = (String) message.get("content");

                        // Handle task-related actions based on GPT response
                        return processTaskAction(userPrompt, content).trim();
                    }
                }
            }
        } catch (Exception e) {
            return "Error: Could not get a valid response from GPT. Please try again.";
        }

        return "Error: Could not get a valid response from GPT.";
    }
    // Process task-related actions based on GPT response
    private String processTaskAction(String userPrompt, String gptResponse) {
        try {
            // Parse the GPT response as JSON
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> actionDetails = objectMapper.readValue(gptResponse, Map.class);
            String action = actionDetails.get("action");
            if ("add".equalsIgnoreCase(action)) {
                String title = actionDetails.get("title");
                String description = actionDetails.get("description");
                String priority = actionDetails.get("priority");
                String dueDateString = actionDetails.get("dueDate"); // Extract the duedate field

                // Parse the duedate string to a LocalDate or LocalDateTime object
                // Define the expected date-time format
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

                // Parse the dueDate string to a LocalDateTime object
                LocalDateTime dueDate = null;
                if (dueDateString != null && !dueDateString.isEmpty()) {
                    try {
                        dueDate = LocalDateTime.parse(dueDateString, formatter);
                    } catch (DateTimeParseException e) {
                        // Handle the exception, e.g., log an error or set a default value
                        System.err.println("Invalid date format: " + dueDateString);
                    }
                }
                // Create and save the task
                taskService.createTask(new Task(title, description, dueDate, priority, null));
                return "Task added successfully: " + title;
            } else if ("delete".equalsIgnoreCase(action)) {
                Long taskId = Long.valueOf(actionDetails.get("taskId"));

                // Delete the task
                taskService.deleteTask(taskId);
                return "Task deleted successfully: ID " + taskId;
            } else {
                return "No task action recognized.";
            }
        } catch (Exception e) {
            return "Error: Unable to process GPT response. Please check the response format.";
        }
    }
}
