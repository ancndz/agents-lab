package ru.autkaev.agents.techretail.customer;

import jade.core.AID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.autkaev.agents.techretail.retailer.TechRetailerAgentGui;
import ru.autkaev.agents.techretail.smartphone.Smartphone;
import ru.autkaev.agents.techretail.smartphone.SmartphoneOs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

/**
 * Окно для работы с покупателем.
 *
 * @author Anton Utkaev
 * @since 2022.06.12
 */
public class CustomerAgentGui extends JFrame {

    private static final Logger LOG = LoggerFactory.getLogger(TechRetailerAgentGui.class);

    private JTextField nameField;

    private JTextField ramField;

    private JTextField cpuField;

    private JTextField priceField;

    private JComboBox<SmartphoneOs> osJComboBox;

    private final JButton addButton;

    private JProgressBar progressBar;

    CustomerAgentGui(final CustomerAgent customerAgent) {
        super(customerAgent.getLocalName());
        // Printout a welcome message
        LOG.info("Open buyer {} frame.", customerAgent.getName());

        final JPanel panel = addLabels();
        getContentPane().add(panel, BorderLayout.CENTER);

        addButton = new JButton("Request");

        addButton.addActionListener(action -> {
            try {
                customerAgent.startBuying(new Smartphone().setName(nameField.getText().trim())
                        .setInstalledRam((ramField.getText().trim()))
                        .setCpuSpeed(cpuField.getText().trim())
                        .setSmartphoneOs((SmartphoneOs) osJComboBox.getSelectedItem())
                        .setPrice(Double.parseDouble(priceField.getText().trim())));
                waitForBuyStateStart();
            } catch (Exception e) {
                LOG.error(e.getMessage());
                JOptionPane.showMessageDialog(CustomerAgentGui.this,
                        "Invalid values. " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        setResizable(false);

        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(final WindowEvent e) {
                customerAgent.doDelete();
            }
        });
    }

    /**
     * Показ окна с информацией об успешной продаже.
     * 
     * @param smartphone
     *            информация о смартфоне
     * @param seller
     *            информация о продавце
     */
    public void showDoneDialog(final Smartphone smartphone, final AID seller) {
        progressBar.setMaximum(100);
        progressBar.setValue(100);
        progressBar.setString("Done!");
        progressBar.setStringPainted(true);
        JOptionPane.showMessageDialog(this,
                String.format("Search complete!\n Customer bought: %s from seller %s",
                        smartphone.toString(),
                        seller.getName()),
                "Done",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Отключение всех элементов.
     */
    private void waitForBuyStateStart() {
        JPanel buttonPanel = new JPanel();
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        buttonPanel.add(progressBar);
        setTitle("Search in progress...");
        getContentPane().add(buttonPanel, BorderLayout.NORTH);

        this.nameField.setEditable(false);
        this.ramField.setEditable(false);
        this.cpuField.setEditable(false);
        this.osJComboBox.setEnabled(false);
        this.priceField.setEditable(false);
        this.addButton.setEnabled(false);

        revalidate();
    }

    /**
     * Добавление полей для ввода.
     * 
     * @return панель с полями
     */
    private JPanel addLabels() {
        JPanel panel = new JPanel();

        panel.setLayout(new GridLayout(5, 2));

        panel.add(new JLabel("Model name:"));
        nameField = new JTextField(15);
        panel.add(nameField);

        panel.add(new JLabel("Installed RAM, Mb:"));
        ramField = new JTextField(15);
        panel.add(ramField);

        panel.add(new JLabel("CPU speed, hz:"));
        cpuField = new JTextField(15);
        panel.add(cpuField);

        panel.add(new JLabel("OS:"));
        osJComboBox = new JComboBox<>(SmartphoneOs.values());
        osJComboBox.insertItemAt(null, 0);
        panel.add(osJComboBox);

        panel.add(new JLabel("Price:"));
        priceField = new JTextField(15);
        panel.add(priceField);

        return panel;
    }

    public void showGui() {
        pack();
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int) screenSize.getWidth() / 2;
        int centerY = (int) screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        super.setVisible(true);
    }

}
