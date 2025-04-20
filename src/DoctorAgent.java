import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.json.JSONArray;

import java.util.LinkedHashMap;
import java.util.Map;

public class DoctorAgent extends Agent {
    private final Map<String, Boolean> slots = new LinkedHashMap<>();

    @Override
    protected void setup() {
        // Initialize available time slots
        slots.put("Tomorrow 10:30AM", true);
        slots.put("Tomorrow 11:00AM", true);
        slots.put("Tomorrow 11:30AM", true);
        slots.put("Tomorrow 2:00PM", true);

        System.out.println(getLocalName() + ": Starting with slots " + slots);

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) {
                    block();
                    return;
                }

                // 1) Provide full list of available slots
                if (msg.getPerformative() == ACLMessage.QUERY_REF
                        && "availableSlots".equals(msg.getContent())) {

                    JSONArray available = new JSONArray();
                    for (Map.Entry<String, Boolean> entry : slots.entrySet()) {
                        if (entry.getValue()) {
                            available.put(entry.getKey());
                        }
                    }

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(available.toString());
                    send(reply);

                    System.out.println(getLocalName() + ": Sent available slots → " + available);
                }

                // 2) Book a chosen slot
                else if (msg.getPerformative() == ACLMessage.REQUEST
                        && msg.getContent().startsWith("bookSlot:")) {

                    String chosenSlot = msg.getContent()
                            .substring("bookSlot:".length()).trim();
                    ACLMessage reply = msg.createReply();

                    if (slots.getOrDefault(chosenSlot, false)) {
                        slots.put(chosenSlot, false);  // Mark as booked
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContent("confirmed:" + chosenSlot);

                        System.out.println(getLocalName()
                                + ": Booked slot → " + chosenSlot);
                    } else {
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("unavailable:" + chosenSlot);

                        System.out.println(getLocalName()
                                + ": Slot unavailable → " + chosenSlot);
                    }
                    send(reply);
                }
            }
        });
    }
}
