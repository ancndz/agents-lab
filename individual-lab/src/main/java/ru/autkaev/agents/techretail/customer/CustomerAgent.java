package ru.autkaev.agents.techretail.customer;

import static ru.autkaev.agents.techretail.ServiceDescriptionType.SMARTPHONE_SELL;
import static ru.autkaev.agents.techretail.ServiceDescriptionType.SMARTPHONE_SELL_CONVERSATION_ID;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.autkaev.agents.techretail.smartphone.Smartphone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Агент покупатель.
 *
 * @author Anton Utkaev
 * @since 2022.06.12
 */
public class CustomerAgent extends Agent {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerAgent.class);

    /**
     * The list of known seller agents.
     */
    private final List<AID> sellerAgents = new ArrayList<>();

    private Smartphone wantedSmartphone;

    private CustomerAgentGui gui;

    @Override
    protected void setup() {
        gui = new CustomerAgentGui(this);
        gui.showGui();
        // Printout a welcome message
        LOG.info("Hello! Buyer-agent {} is ready.", getAID().getName());
    }

    public void startBuying(final Smartphone smartphone) {
        if (smartphone == null) {
            LOG.info("No target  specified");
            doDelete();
            return;
        }
        wantedSmartphone = smartphone;

        LOG.info("Target smartphone is {}", smartphone);

        // Add a TickerBehaviour that schedules a request to seller agents every 10 sec
        addBehaviour(new TickerBehaviour(this, 10000) {

            @Override
            protected void onTick() {
                LOG.info("Trying to buy {}", smartphone);
                // Update the list of seller agents
                final DFAgentDescription agentDescription = new DFAgentDescription();
                final ServiceDescription serviceDescription = new ServiceDescription();
                serviceDescription.setType(SMARTPHONE_SELL);
                agentDescription.addServices(serviceDescription);
                final List<DFAgentDescription> searchResult = new ArrayList<>();
                try {
                    searchResult.addAll(Arrays.asList(DFService.search(myAgent, agentDescription)));
                } catch (FIPAException fe) {
                    LOG.error(fe.getMessage());
                    fe.printStackTrace();
                }
                searchResult.forEach(dfAgentDescription -> sellerAgents.add(dfAgentDescription.getName()));
                LOG.info("Found the following seller agents: {}",
                        sellerAgents.stream().map(AID::getName).collect(Collectors.joining(", ")));

                // Perform the request
                myAgent.addBehaviour(new CustomerAgent.RequestPerformer());
            }
        });
    }

    @Override
    protected void takeDown() {
        // Printout a dismissal message
        gui.dispose();
        LOG.info("Buyer-agent {} terminating.", getAID().getName());
    }

    /**
     * Inner class RequestPerformer. This is the behaviour used by Book-buyer agents to request seller agents the target
     * book.
     */
    private class RequestPerformer extends Behaviour {

        private AID bestSeller; // The agent who provides the best offer

        private Smartphone bestSmartphone; // The best offered price

        private int step = 0;

        public void action() {

            int repliesCnt = 0; // The counter of replies from seller agents

            MessageTemplate messageTemplate = null; // The template to receive replies

            switch (step) {
                case 0:
                    // Send the aclMessage to all sellers
                    final ACLMessage aclMessage = new ACLMessage(ACLMessage.CFP);
                    sellerAgents.forEach(aclMessage::addReceiver);

                    aclMessage.setByteSequenceContent(SerializationUtils.serialize(wantedSmartphone));
                    aclMessage.setConversationId(SMARTPHONE_SELL_CONVERSATION_ID);
                    aclMessage.setReplyWith("aclMessage " + UUID.randomUUID().toString().substring(0, 4));
                    myAgent.send(aclMessage);

                    // Prepare the template to get proposals
                    messageTemplate =
                            MessageTemplate.and(MessageTemplate.MatchConversationId(SMARTPHONE_SELL_CONVERSATION_ID),
                                    MessageTemplate.MatchInReplyTo(aclMessage.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    // Receive all proposals/refusals from seller agents
                    ACLMessage reply = myAgent.receive(messageTemplate);
                    if (reply != null) {
                        // Reply received
                        if (Integer.valueOf(ACLMessage.PROPOSE).equals(reply.getPerformative())) {
                            // This is an offer
                            final ArrayList<Smartphone> smartphones =
                                    SerializationUtils.deserialize(reply.getByteSequenceContent());
                            // самый дешевый из подходящих
                            final Smartphone cheapestSmartphone =
                                    smartphones.stream().min(Comparator.comparing(Smartphone::getPrice)).orElse(null);
                            if (bestSeller == null
                                    || bestSmartphone == null
                                    || cheapestSmartphone.getPrice().compareTo(bestSmartphone.getPrice()) < 0) {
                                // This is the best offer at present
                                bestSmartphone = cheapestSmartphone;
                                bestSeller = reply.getSender();
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= sellerAgents.size()) {
                            // We received all replies
                            step = 2;
                        }
                    } else {
                        block();
                    }
                    break;
                case 2:
                    // Send the purchase order to the seller that provided the best offer
                    final ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestSeller);
                    order.setContent(bestSmartphone.getName());
                    order.setConversationId(SMARTPHONE_SELL_CONVERSATION_ID);
                    order.setReplyWith("aclMessage " + UUID.randomUUID().toString().substring(0, 4));
                    myAgent.send(order);

                    // Prepare the template to get the purchase order reply
                    messageTemplate =
                            MessageTemplate.and(MessageTemplate.MatchConversationId(SMARTPHONE_SELL_CONVERSATION_ID),
                                    MessageTemplate.MatchInReplyTo(order.getReplyWith()));

                    step = 3;
                    break;
                case 3:
                    // Receive the purchase order reply
                    reply = myAgent.receive(messageTemplate);
                    if (reply != null) {
                        // Purchase order reply received
                        if (Integer.valueOf(ACLMessage.INFORM).equals(reply.getPerformative())) {
                            // Purchase successful. We can terminate
                            LOG.info("{} successfully purchased from agent {}\nPrice = {}",
                                    bestSmartphone.getName(),
                                    reply.getSender().getName(),
                                    bestSmartphone.getPrice());
                            gui.showDoneDialog(bestSmartphone, reply.getSender());
                            myAgent.doDelete();
                        } else {
                            LOG.info("Attempt failed: requested book already sold.");
                        }
                        step = 4;
                    } else {
                        block();
                    }
                    break;
            }
        }

        @Override
        public boolean done() {
            if (this.step == 2 && this.bestSeller == null) {
                LOG.info("Attempt failed: {} not available for sale", wantedSmartphone);
            }
            return ((step == 2 && bestSeller == null) || step == 4);
        }
    }
}
