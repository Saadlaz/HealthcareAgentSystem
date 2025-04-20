import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.LinkedHashMap;
import java.util.Map;

public class HospitalAgent extends Agent {
    // Map of ICU names → availability
    private final Map<String,Boolean> icuBeds = new LinkedHashMap<>();

    @Override
    protected void setup() {
        // ICU-A is free, ICU-B is busy
        icuBeds.put("ICU-A", true);
        icuBeds.put("ICU-B", false);
        System.out.println(getLocalName() + ": ICU map = " + icuBeds);

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) {
                    block();
                    return;
                }

                // 1) Check availability
                if (msg.getPerformative() == ACLMessage.QUERY_IF
                        && "ICU availability?".equals(msg.getContent()))
                {
                    boolean anyFree = icuBeds.containsValue(true);
                    System.out.println(getLocalName()
                            + ": any ICU free? " + anyFree);

                    ACLMessage reply = new ACLMessage(
                            anyFree ? ACLMessage.CONFIRM : ACLMessage.DISCONFIRM
                    );
                    reply.addReceiver(new AID("coordinator", AID.ISLOCALNAME));
                    // include exact flag
                    reply.setContent("ICUAvailable=" + anyFree);
                    send(reply);
                }

                // 2) Book one if free
                else if (msg.getPerformative() == ACLMessage.REQUEST
                        && "assignToICU".equals(msg.getContent()))
                {
                    String booked = null;
                    for (Map.Entry<String,Boolean> e : icuBeds.entrySet()) {
                        if (e.getValue()) {
                            booked = e.getKey();
                            icuBeds.put(booked, false);
                            break;
                        }
                    }

                    ACLMessage reply = new ACLMessage(
                            booked != null ? ACLMessage.INFORM : ACLMessage.FAILURE
                    );
                    reply.addReceiver(new AID("coordinator", AID.ISLOCALNAME));
                    if (booked != null) {
                        System.out.println(getLocalName()
                                + ": Booked " + booked + " → " + icuBeds);
                        reply.setContent("ICU-assigned=true;unit=" + booked);
                    } else {
                        System.out.println(getLocalName()
                                + ": No ICU left → " + icuBeds);
                        reply.setContent("ICU-assigned=false");
                    }
                    send(reply);
                }
            }
        });
    }
}
