package ru.autkaev.agents.techretail.retailer;

import static ru.autkaev.agents.techretail.ServiceDescriptionType.SMARTPHONE_SELL;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Агент продавец.
 *
 * @author Anton Utkaev
 * @since 2022.06.12
 */
public class TechRetailerAgent extends Agent {

    private static final Logger LOG = LoggerFactory.getLogger(TechRetailerAgent.class);

    private final List<Smartphone> smartphoneList = new ArrayList<>();

    private TechRetailerAgentGui gui;

    @Override
    protected void setup() {

        gui = new TechRetailerAgentGui(this);
        gui.showGui();

        // Register the tech-selling service in the yellow pages
        final DFAgentDescription dfAgentDescription = new DFAgentDescription();
        dfAgentDescription.setName(this.getAID());
        final ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType(SMARTPHONE_SELL);
        serviceDescription.setName("JADE-tech-trading");
        dfAgentDescription.addServices(serviceDescription);
        try {
            DFService.register(this, dfAgentDescription);
            LOG.info("New retailer registered: [name: {}]", this.getName());
        } catch (FIPAException fe) {
            LOG.error(fe.getMessage());
            fe.printStackTrace();
        }

        // Add the behaviour serving queries from buyer agents
        addBehaviour(new TechRetailerAgent.OfferRequestsServer());

        // Add the behavior telling log its catalogue
        addBehaviour(new TickerBehaviour(this, 20000) {

            @Override
            protected void onTick() {
                if (!smartphoneList.isEmpty()) {
                    LOG.info("{} is selling smartphones: {}",
                            getLocalName(),
                            smartphoneList.stream().map(Smartphone::toString).collect(Collectors.joining(", ")));
                }
            }

        });

        // Add the behaviour serving purchase orders from buyer agents
        addBehaviour(new TechRetailerAgent.PurchaseOrdersServer());
    }

    @Override
    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            LOG.error(fe.getMessage());
            fe.printStackTrace();
        }
        // Close the GUI
        gui.dispose();
        // Printout a dismissal message
        LOG.info("Seller-agent {} terminating.", getAID().getName());
    }

    public void addSmartphoneList(final Smartphone smartphone) {
        this.smartphoneList.add(smartphone);
        LOG.info("Smartphone inserted into catalogue. Smartphone = {}", smartphone.toString());
    }

    /**
     * Inner class OfferRequestsServer. This is the behaviour used by Book-seller agents to serve incoming requests for
     * offer from buyer agents. If the requested book is in the local catalogue the seller agent replies with a PROPOSE
     * message specifying the price. Otherwise a REFUSE message is sent back.
     */
    private class OfferRequestsServer extends CyclicBehaviour {

        @Override
        public void action() {
            final MessageTemplate messageTemplate = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            final ACLMessage aclMessage = myAgent.receive(messageTemplate);
            if (aclMessage != null) {
                // CFP Message received. Process it
                final byte[] byteSequenceContent = aclMessage.getByteSequenceContent();

                // получаем требуемый смартфон после из запроса
                final Smartphone smartphone = SerializationUtils.deserialize(byteSequenceContent);

                final ACLMessage reply = aclMessage.createReply();

                // получаем список подходящих смартфонов
                final ArrayList<Smartphone> matchesSmartphoneList = getMatchesSmartphones(smartphone);

                if (!matchesSmartphoneList.isEmpty()) {
                    // The requested book is available for sale. Reply with the price
                    reply.setPerformative(ACLMessage.PROPOSE);
                    final byte[] contentBytes = SerializationUtils.serialize(matchesSmartphoneList);
                    if (contentBytes != null) {
                        reply.setByteSequenceContent(contentBytes);
                    } else {
                        reply.setContent("something went wrong during create byte array");
                    }
                } else {
                    // The requested book is NOT available for sale.
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }

        /**
         * Получение списка подходящих смартфонов под критерии пользователя.
         * 
         * @param requestedSmartphone
         *            критерии пользователя.
         * @return список подходящих смартфонов
         */
        private ArrayList<Smartphone> getMatchesSmartphones(final Smartphone requestedSmartphone) {

            return smartphoneList.stream().filter(smartphone -> {
                final Boolean nameMatch = Optional.ofNullable(requestedSmartphone.getName())
                        .map(name -> smartphone.getName().toLowerCase().contains(name.toLowerCase()))
                        .orElse(Boolean.TRUE);

                final Boolean ramMatch = Optional.ofNullable(requestedSmartphone.getInstalledRam())
                        .map(ram -> smartphone.getInstalledRam().compareTo(ram) >= 0)
                        .orElse(Boolean.TRUE);

                final Boolean cpuMatch = Optional.ofNullable(requestedSmartphone.getCpuSpeed())
                        .map(cpu -> smartphone.getCpuSpeed().compareTo(cpu) >= 0)
                        .orElse(Boolean.TRUE);

                final Boolean osMatch = Optional.ofNullable(requestedSmartphone.getSmartphoneOs())
                        .map(os -> os.equals(smartphone.getSmartphoneOs()))
                        .orElse(Boolean.TRUE);

                final Boolean priceMatch = Optional.ofNullable(requestedSmartphone.getPrice())
                        .map(price -> smartphone.getPrice().compareTo(price) <= 0)
                        .orElse(Boolean.TRUE);

                return nameMatch && ramMatch && cpuMatch && osMatch && priceMatch;
            }).collect(Collectors.toCollection(ArrayList::new));
        }
    }

    /**
     * Inner class PurchaseOrdersServer. This is the behaviour used by Book-seller agents to serve incoming offer
     * acceptances (i.e. purchase orders) from buyer agents. The seller agent removes the purchased book from its
     * catalogue and replies with an INFORM message to notify the buyer that the purchase has been successfully
     * completed.
     */
    private class PurchaseOrdersServer extends CyclicBehaviour {

        @Override
        public void action() {
            final MessageTemplate messageTemplate = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            final ACLMessage aclMessage = myAgent.receive(messageTemplate);
            if (aclMessage != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                final String name = aclMessage.getContent();
                final ACLMessage reply = aclMessage.createReply();

                final boolean removed = smartphoneList.removeIf(smartphone -> smartphone.getName().equals(name));

                if (removed) {
                    reply.setPerformative(ACLMessage.INFORM);
                    LOG.info("{} sold to agent {}", name, aclMessage.getSender().getName());
                } else {
                    // The requested book has been sold to another buyer in the meanwhile .
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);

                // завершение работы если все распродано
                if (smartphoneList.isEmpty()) {
                    TechRetailerAgent.this.doDelete();
                }
            } else {
                block();
            }
        }
    }

}
