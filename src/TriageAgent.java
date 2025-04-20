

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TriageAgent extends Agent {
    // Name of the env‑var holding your API key
    private static final String API_KEY_ENV_VAR = "GEMINI_API_KEY";
    // Endpoint for Gemini 2.0 Flash generateContent
    private static final String API_URL_FORMAT =
            "https://generativelanguage.googleapis.com/v1beta/models/"
                    + "gemini-2.0-flash:generateContent?key=%s";

    private String apiKey;

    @Override
    protected void setup() {
        System.out.println(getLocalName() + ": Starting setup...");

        // 1. Load the API key from env‑var
        apiKey = System.getenv(API_KEY_ENV_VAR);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.printf(
                    "%s: FATAL ERROR - please set environment variable %s to your API key%n",
                    getLocalName(), API_KEY_ENV_VAR);
            doDelete();               // Terminate agent
            return;
        }
        System.out.println(getLocalName() + ": API Key loaded successfully.");

        // 2. Add behaviour to handle triage requests
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null
                        && msg.getPerformative() == ACLMessage.REQUEST
                        && msg.getContent() != null) {

                    String symptoms = msg.getContent().trim();
                    System.out.println(getLocalName() + ": Received symptoms → " + symptoms);

                    // 3. Classify severity via Gemini API
                    String severity = classifySymptoms(symptoms);

                    // 4. Reply back to CoordinatorAgent
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("severityLevel=" + severity);
                    send(reply);

                    System.out.println(getLocalName() + ": Replied severityLevel=" + severity);
                } else {
                    block();
                }
            }
        });
    }

    private String classifySymptoms(String symptoms) {
        HttpURLConnection conn = null;
        try {
            // Build URL with your API key
            URL url = new URL(String.format(API_URL_FORMAT, apiKey));
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            // Build request body for generateContent
            JSONObject body = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();



            String promptText = "You are a virtual medical triage assistant. "
                    + "Your task is to classify the severity of the following symptoms "
                    + "as either High, Medium, or Low. Do NOT explain. Just reply with one word. "
                    + "Symptoms: " + symptoms;
            part.put("text", promptText);
            parts.put(part);

            content.put("parts", parts);
            contents.put(content);
            body.put("contents", contents);

            JSONObject genConfig = new JSONObject();
            genConfig.put("temperature", 0.2);
            genConfig.put("candidateCount", 1);
            body.put("generationConfig", genConfig);

            // Send JSON payload
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            // Read response
            int code = conn.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    (code >= 200 && code < 300) ?
                            conn.getInputStream() : conn.getErrorStream(),
                    StandardCharsets.UTF_8));
            StringBuilder resp = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) resp.append(line);
            reader.close();

            if (code < 200 || code >= 300) {
                System.err.printf("%s: API error %d → %s%n",
                        getLocalName(), code, resp);
                return "Error";
            }

            // Parse the "candidates" → first candidate → parts → text
            JSONObject json = new JSONObject(resp.toString());
            JSONArray candidates = json.getJSONArray("candidates");
            if (candidates.isEmpty()) {
                System.err.println(getLocalName() + ": No candidates returned");
                return "Error";
            }

            JSONObject first = candidates.getJSONObject(0);
            JSONObject contentObj = first.getJSONObject("content");
            JSONArray returnedParts = contentObj.getJSONArray("parts");
            String text = returnedParts.getJSONObject(0).getString("text").trim().toLowerCase();

            // Normalize to High/Medium/Low
            if (text.contains("high"))   return "High";
            if (text.contains("medium")) return "Medium";
            return "Low";

        } catch (Exception e) {
            e.printStackTrace();
            return "Error";
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    @Override
    protected void takeDown() {
        System.out.println(getLocalName() + ": Terminating.");
        super.takeDown();
    }
}
