import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class SchedulingAgent extends Agent {
    @Override
    protected void setup() {
        System.out.println(getLocalName() + ": Starting SchedulingAgent");

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();

                if (msg != null) {
                    // Step 1: Patient requests to schedule → Ask doctor for ALL available slots
                    if (msg.getPerformative() == ACLMessage.REQUEST
                            && msg.getContent().equals("scheduleAppointment")) {
                        System.out.println(getLocalName() + ": Requesting doctor for all available slots...");

                        ACLMessage queryMsg = new ACLMessage(ACLMessage.QUERY_REF);
                        queryMsg.addReceiver(new AID("doctor", AID.ISLOCALNAME));
                        queryMsg.setProtocol("fipa-query");
                        queryMsg.setContent("listAvailableSlots");
                        send(queryMsg);
                    }

                    // Step 2: Doctor sends list of slots → Forward to Coordinator
                    else if (msg.getPerformative() == ACLMessage.INFORM
                            && msg.getContent().startsWith("slots=")) {
                        System.out.println(getLocalName() + ": Received slot list from DoctorAgent");

                        ACLMessage forward = new ACLMessage(ACLMessage.INFORM);
                        forward.addReceiver(new AID("coordinator", AID.ISLOCALNAME));
                        forward.setContent(msg.getContent()); // e.g., slots=Tomorrow 10:30AM;Tomorrow 11:00AM
                        send(forward);
                        System.out.println(getLocalName() + ": Forwarded slot list to CoordinatorAgent");
                    }
                } else {
                    block();
                }
            }

        });
    }
}
