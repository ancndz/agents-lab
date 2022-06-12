package ru.autkaev.agents.booktrading.seller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Меню продавца.
 *
 * @author Anton Utkaev
 * @since 2022.06.12
 */
public class BookSellerGui extends JFrame {

    private static final Logger LOG = LoggerFactory.getLogger(BookSellerGui.class);

    BookSellerGui(final BookSellerAgent bookSellerAgent) {
        super(bookSellerAgent.getName());
        // Printout a welcome message
        LOG.info("Open seller {} frame.", bookSellerAgent.getName());

        final JTextField titleField;

        final JTextField priceField;

        JPanel panel = new JPanel();

        panel.setLayout(new GridLayout(2, 2));

        panel.add(new JLabel("Book title:"));
        titleField = new JTextField(15);
        panel.add(titleField);

        panel.add(new JLabel("Price:"));
        priceField = new JTextField(15);
        panel.add(priceField);
        getContentPane().add(panel, BorderLayout.CENTER);

        JButton addButton = new JButton("Add");
        addButton.addActionListener(ev -> {
            try {
                final String titleString = titleField.getText().trim();
                final String priceString = priceField.getText().trim();
                bookSellerAgent.updateCatalogue(titleString, Double.parseDouble(priceString));
                titleField.setText("");
                priceField.setText("");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(BookSellerGui.this,
                        "Invalid values. " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        panel = new JPanel();
        panel.add(addButton);
        getContentPane().add(panel, BorderLayout.SOUTH);

        // Make the agent terminate when the user closes
        // the GUI using the button in the upper right corner
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                bookSellerAgent.doDelete();
            }
        });

        setResizable(false);
    }

    public void showGui() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int) screenSize.getWidth() / 2;
        int centerY = (int) screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        super.setVisible(true);
    }
}
