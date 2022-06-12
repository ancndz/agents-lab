package ru.autkaev.agents.simple.receiver;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Получатель сообщений.
 *
 * @author Anton Utkaev
 * @since 2022.06.10
 */
public class ReceiverAgent extends Agent {

    private static final Logger LOG = LoggerFactory.getLogger(ReceiverAgent.class);

    @Override
    protected void setup() {
        LOG.debug("Agent {} is ready.", getAID().getName());
        addBehaviour(new CyclicBehaviour(this) {

            public void action() {
                final ACLMessage aclMessage = receive();
                if (aclMessage != null) {
                    LOG.debug("Agent {} received message: {}", myAgent.getLocalName(), aclMessage.getContent());

                    final ACLMessage reply = aclMessage.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("Pong"); // Содержимое сообщения
                    send(reply); // отправляем сообщения
                }
                block();
            }
        });
    }

}
