package ru.autkaev.agents.booktrading.seller;

import static ru.autkaev.agents.booktrading.DescriptionTypes.BOOK_SELLING_DESC_TYPE;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Агент продавца.
 *
 * @author Anton Utkaev
 * @since 2022.06.12
 */
public class BookSellerAgent extends Agent {

    private static final Logger LOG = LoggerFactory.getLogger(BookSellerAgent.class);

    /**
     * The catalogue of books for sale (maps the title of a book to its price)
     */
    private final Map<String, Double> catalogue = new HashMap<>();

    /**
     * The GUI by means of which the user can add books in the catalogue
     */
    private BookSellerGui myGui;

    @Override
    protected void setup() {

        // Create and show the GUI
        myGui = new BookSellerGui(this);
        myGui.showGui();

        // Register the book-selling service in the yellow pages
        final DFAgentDescription dfAgentDescription = new DFAgentDescription();
        dfAgentDescription.setName(this.getAID());
        final ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType(BOOK_SELLING_DESC_TYPE);
        serviceDescription.setName("JADE-book-trading");
        dfAgentDescription.addServices(serviceDescription);
        try {
            DFService.register(this, dfAgentDescription);
        } catch (FIPAException fe) {
            LOG.error(fe.getMessage());
            fe.printStackTrace();
        }

        // Add the behaviour serving queries from buyer agents
        addBehaviour(new BookSellerAgent.OfferRequestsServer());

        // Add the behavior telling log its catalogue
        addBehaviour(new TickerBehaviour(this, 20000) {

            @Override
            protected void onTick() {
                if (!catalogue.isEmpty()) {
                    LOG.info("Book to sold! {}",
                            catalogue.entrySet()
                                    .stream()
                                    .map(stringDoubleEntry -> String.format("Title: %s, Price: %s",
                                            stringDoubleEntry.getKey(),
                                            stringDoubleEntry.getValue()))
                                    .collect(Collectors.joining("\n")));
                }
            }

        });

        // Add the behaviour serving purchase orders from buyer agents
        addBehaviour(new BookSellerAgent.PurchaseOrdersServer());
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
        myGui.dispose();
        // Printout a dismissal message
        LOG.info("Seller-agent {} terminating.", getAID().getName());
    }

    /**
     * This is invoked by the GUI when the user adds a new book for sale.
     */
    public void updateCatalogue(final String title, final double price) {
        addBehaviour(new OneShotBehaviour() {

            @Override
            public void action() {
                catalogue.put(title.toLowerCase(), price);
                LOG.info("{} inserted into catalogue. Price = {}", title, price);
            }
        });
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
                final String title = aclMessage.getContent().toLowerCase();
                final ACLMessage reply = aclMessage.createReply();

                final Double price = catalogue.get(title);
                if (price != null) {
                    // The requested book is available for sale. Reply with the price
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(price.doubleValue()));
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
    }

    /**
     * Inner class PurchaseOrdersServer. This is the behaviour used by Book-seller agents to serve incoming offer
     * acceptances (i.e. purchase orders) from buyer agents. The seller agent removes the purchased book from its
     * catalogue and replies with an INFORM message to notify the buyer that the purchase has been sucesfully completed.
     */
    private class PurchaseOrdersServer extends CyclicBehaviour {

        @Override
        public void action() {
            final MessageTemplate messageTemplate = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            final ACLMessage aclMessage = myAgent.receive(messageTemplate);
            if (aclMessage != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                final String title = aclMessage.getContent();
                final ACLMessage reply = aclMessage.createReply();

                final Double price = catalogue.remove(title);
                if (price != null) {
                    reply.setPerformative(ACLMessage.INFORM);
                    LOG.info("{} sold to agent {}", title, aclMessage.getSender().getName());
                } else {
                    // The requested book has been sold to another buyer in the meanwhile .
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }
}
