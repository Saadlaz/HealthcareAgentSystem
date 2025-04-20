import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;

public class PatientAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println("✅ " + getLocalName() + ": Starting...");

        // 1) Initial submission of patient case via GUI dialogs
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                String nom = JOptionPane.showInputDialog(null, "Entrez le nom du patient:", "Patient Info", JOptionPane.PLAIN_MESSAGE);
                String prenom = JOptionPane.showInputDialog(null, "Entrez le prénom:", "Patient Info", JOptionPane.PLAIN_MESSAGE);
                String symptomes = JOptionPane.showInputDialog(null, "Entrez les symptômes:", "Patient Info", JOptionPane.PLAIN_MESSAGE);

                if (nom == null || prenom == null || symptomes == null) {
                    System.out.println("❌ Input was cancelled. Agent will not send data.");
                    return;
                }

                JSONObject patientData = new JSONObject();
                patientData.put("type", "submitCase");
                patientData.put("nom", nom);
                patientData.put("prenom", prenom);
                patientData.put("symptomes", symptomes);

                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(new AID("coordinator", AID.ISLOCALNAME));
                msg.setProtocol("fipa-request");
                msg.setContent(patientData.toString());
                send(msg);

                System.out.println("📤 Data sent to CoordinatorAgent: " + patientData);
            }
        });

        // 2) Handle incoming ACLMessages
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
                    String content = msg.getContent();

                    // a) List of slots to choose from
                    if (content.startsWith("chooseSlot:")) {
                        String json = content.substring("chooseSlot:".length());
                        JSONArray options = new JSONArray(json);
                        String[] slots = new String[options.length()];
                        for (int i = 0; i < options.length(); i++) {
                            slots[i] = options.getString(i);
                        }
                        String chosen = (String) JOptionPane.showInputDialog(
                                null,
                                "Veuillez choisir un créneau:\n",
                                "Sélection du créneau",
                                JOptionPane.PLAIN_MESSAGE,
                                null,
                                slots,
                                slots[0]
                        );
                        if (chosen != null) {
                            ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
                            reply.addReceiver(new AID("coordinator", AID.ISLOCALNAME));
                            reply.setContent("selectedSlot:" + chosen);
                            send(reply);
                            System.out.println("📤 Patient selected slot: " + chosen);
                        }
                    }
                    // b) ICU admission notification
                    else if (content.startsWith("admitted to")) {
                        JOptionPane.showMessageDialog(
                                null,
                                "📥 Notification reçue :\n" + content,
                                "Admission ICU",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        System.out.println(getLocalName() + ": " + content);
                    }
                    // c) Appointment confirmation
                    else if (content.startsWith("appointment details:") || content.startsWith("appointment confirmed")) {
                        JOptionPane.showMessageDialog(
                                null,
                                "📥 Notification reçue :\n" + content,
                                "Rendez-vous",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        System.out.println(getLocalName() + ": " + content);
                    }
                } else {
                    block();
                }
            }
        });
    }
}
