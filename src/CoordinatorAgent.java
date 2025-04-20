import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.json.JSONObject;

import java.util.UUID;

public class CoordinatorAgent extends Agent {
    private String caseId = UUID.randomUUID().toString();
    private JSONObject currentPatient;

    @Override
    protected void setup() {
        System.out.println(getLocalName() + ": Starting");

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) {
                    block();
                    return;
                }

                switch (msg.getPerformative()) {
                    // 1) PatientAgent submits the case
                    case ACLMessage.REQUEST:
                        currentPatient = new JSONObject(msg.getContent());
                        String symptoms = currentPatient.getString("symptomes");
                        System.out.println(getLocalName() + ": Received symptoms → " + symptoms);

                        // Forward to TriageAgent
                        ACLMessage toTriage = msg.createReply();
                        toTriage.setPerformative(ACLMessage.REQUEST);
                        toTriage.addReceiver(new AID("triage", AID.ISLOCALNAME));
                        toTriage.setProtocol("triage");
                        toTriage.setContent(symptoms);
                        send(toTriage);
                        break;

                    // 2–8) Everything else comes back as INFORM, CONFIRM or DISCONFIRM
                    case ACLMessage.INFORM:
                    case ACLMessage.CONFIRM:
                    case ACLMessage.DISCONFIRM:
                        String content = msg.getContent();
                        System.out.println(getLocalName() + ": Got "
                                + ACLMessage.getPerformative(msg.getPerformative())
                                + " → " + content);

                        // 2) Triage result
                        if (content.startsWith("severityLevel=")) {
                            String severity = content.split("=")[1];
                            System.out.println(getLocalName() + ": Triage severity = " + severity);

                            if ("High".equalsIgnoreCase(severity)) {
                                // Ask ICU availability
                                ACLMessage askIcu = new ACLMessage(ACLMessage.QUERY_IF);
                                askIcu.addReceiver(new AID("hospital", AID.ISLOCALNAME));
                                askIcu.setContent("ICU availability?");
                                send(askIcu);
                            } else {
                                // Low/Medium → ask doctor for slot list
                                ACLMessage askSlots = new ACLMessage(ACLMessage.QUERY_REF);
                                askSlots.addReceiver(new AID("doctor", AID.ISLOCALNAME));
                                askSlots.setContent("availableSlots");
                                send(askSlots);
                            }
                        }

                        // 3) ICU availability reply (CONFIRM / DISCONFIRM)
                        else if (content.startsWith("ICUAvailable=")) {
                            boolean free = Boolean.parseBoolean(content.split("=")[1]);
                            System.out.println(getLocalName() + ": ICU free? " + free);
                            if (free) {
                                // Book the ICU
                                ACLMessage book = new ACLMessage(ACLMessage.REQUEST);
                                book.addReceiver(new AID("hospital", AID.ISLOCALNAME));
                                book.setContent("assignToICU");
                                send(book);
                            } else {
                                // No ICU → fallback to doctor slots
                                ACLMessage askSlots = new ACLMessage(ACLMessage.QUERY_REF);
                                askSlots.addReceiver(new AID("doctor", AID.ISLOCALNAME));
                                askSlots.setContent("availableSlots");
                                send(askSlots);
                            }
                        }

                        // 4) DoctorAgent delivers the list of slots (JSON array)
                        else if (content.startsWith("[") && content.contains("\"")) {
                            // Forward list to patient for selection
                            ACLMessage toPatient = new ACLMessage(ACLMessage.INFORM);
                            toPatient.addReceiver(new AID("patient", AID.ISLOCALNAME));
                            toPatient.setContent("chooseSlot:" + content);
                            send(toPatient);
                        }

                        // 5) Patient picks a slot
                        else if (content.startsWith("selectedSlot:")) {
                            String selected = content.replace("selectedSlot:", "").trim();
                            System.out.println(getLocalName() + ": Patient selected → " + selected);

                            // Send booking request to doctor
                            ACLMessage bookSlot = new ACLMessage(ACLMessage.REQUEST);
                            bookSlot.addReceiver(new AID("doctor", AID.ISLOCALNAME));
                            bookSlot.setContent("bookSlot:" + selected);
                            send(bookSlot);
                        }

                        // 6) Doctor confirms or rejects the booking
                        else if (content.startsWith("confirmed:")) {
                            String slot = content.split(":", 2)[1];
                            ACLMessage toPatient = new ACLMessage(ACLMessage.INFORM);
                            toPatient.addReceiver(new AID("patient", AID.ISLOCALNAME));
                            toPatient.setContent("appointment confirmed at " + slot);
                            send(toPatient);
                        }
                        else if (content.startsWith("unavailable:")) {
                            ACLMessage toPatient = new ACLMessage(ACLMessage.INFORM);
                            toPatient.addReceiver(new AID("patient", AID.ISLOCALNAME));
                            toPatient.setContent("Chosen slot no longer available. Please retry.");
                            send(toPatient);
                        }
                        break;

                    default:
                        // We don’t expect other performatives here
                }
            }
        });
    }
}
