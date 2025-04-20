import jade.core.Runtime;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.Random;

/*class MLBackend {
    static String processSymptoms(String symptoms) {
        return new Random().nextBoolean() ? "High" : "Low"; // Simulates severity level
    }
}*/

public class HealthcareAgentSystem {
    public static void main(String[] args) {
        // Start JADE runtime
        Runtime rt = Runtime.instance();
        // Create a Profile for the main container with GUI enabled
        ProfileImpl profile = new ProfileImpl();
        profile.setParameter(ProfileImpl.MAIN_HOST, "localhost");
        profile.setParameter(ProfileImpl.MAIN_PORT, "1099");
        profile.setParameter(ProfileImpl.GUI, "true"); // Enable JADE RMA GUI
        ContainerController mainContainer = rt.createMainContainer(profile);

        // Create and start agents, including Sniffer
        try {
            // Start regular agents
            AgentController patient = mainContainer.createNewAgent("patient", "PatientAgent", null);
            AgentController coordinator = mainContainer.createNewAgent("coordinator", "CoordinatorAgent", null);
            AgentController triage = mainContainer.createNewAgent("triage", "TriageAgent", null);
            AgentController hospital = mainContainer.createNewAgent("hospital", "HospitalAgent", null);
            AgentController doctor = mainContainer.createNewAgent("doctor", "DoctorAgent", null);
            AgentController scheduling = mainContainer.createNewAgent("scheduling", "SchedulingAgent", null);

            // Start Sniffer Agent to monitor all agents
            AgentController sniffer = mainContainer.createNewAgent("sniffer", "jade.tools.sniffer.Sniffer", new String[] {
                "patient;coordinator;triage;hospital;doctor;scheduling" // List of agents to sniff
            });

            // Start all agents
            patient.start();
            coordinator.start();
            triage.start();
            hospital.start();
            doctor.start();
            scheduling.start();
            sniffer.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}