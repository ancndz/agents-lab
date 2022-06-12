package ru.autkaev.agents.simple.source;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Агент, который рассылает сообщения.
 *
 * @author Anton Utkaev
 * @since 2022.06.11
 */
public class SourceAgent extends Agent {

    private static final Logger LOG = LoggerFactory.getLogger(SourceAgent.class);

    @Override
    protected void setup() {
        LOG.debug("Agent {} is ready.", getAID().getName());
        addBehaviour(new CyclicBehaviour() {

            @Override
            public void action() {
                final ACLMessage message = receive();

                if (message != null) {
                    LOG.debug("Agent {} received message: {}", myAgent.getLocalName(), message.getContent());
                }
                block();
            }
        });

        final List<AMSAgentDescription> agentList;
        final SearchConstraints constraints = new SearchConstraints();
        constraints.setMaxResults(-1L);
        try {
            agentList = Arrays.asList(AMSService.search(this, new AMSAgentDescription(), constraints));
        } catch (FIPAException e) {
            LOG.error(e.getMessage());
            throw new RuntimeException("Exception while searching AMS!", e);
        }

        agentList.forEach(amsAgentDescription -> {
            final AID agentID = amsAgentDescription.getName();
            final ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(agentID); // id агента, которому отправляем сообщение
            msg.setLanguage("English"); // Язык
            msg.setContent("Ping"); // Содержимое сообщения
            send(msg); // отправляем сообщение
        });
    }
}
