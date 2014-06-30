
package org.newdawn.slick.font.effects;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

import org.newdawn.slick.font.GlyphPage;
import org.newdawn.slick.font.effects.ConfigurableEffect.Value;

/**
 * Provides utility methods for effects.
 * 
 * @author Nathan Sweet <misc@n4te.com>
 */
public class EffectUtil {
	/** A graphics 2D temporary surface to be used when generating effects */
	static private BufferedImage scratchImage = new BufferedImage(GlyphPage.MAX_GLYPH_SIZE, GlyphPage.MAX_GLYPH_SIZE,
		BufferedImage.TYPE_INT_ARGB);

	/**
	 * Returns an image that can be used by effects as a temp image.
	 * 
	 * @return The scratch image used for temporary operations
	 */
	static public BufferedImage getScratchImage() {
		Graphics2D g = (Graphics2D)scratchImage.getGraphics();
		g.setComposite(AlphaComposite.Clear);
		g.fillRect(0, 0, GlyphPage.MAX_GLYPH_SIZE, GlyphPage.MAX_GLYPH_SIZE);
		g.setComposite(AlphaComposite.SrcOver);
		g.setColor(java.awt.Color.white);
		return scratchImage;
	}

	/**
	 * Prompts the user for a colour value
	 * 
	 * @param name Thename of the value being configured
	 * @param currentValue The default value that should be selected
	 * @return The value selected
	 */
	static public Value colorValue(String name, Color currentValue) {
		return new DefaultValue(name, EffectUtil.toString(currentValue)) {
			public void showDialog () {
				Color newColor = JColorChooser.showDialog(null, "Choose a color", EffectUtil.fromString(value));
				if (newColor != null) value = EffectUtil.toString(newColor);
			}

			public Object getObject () {
				return EffectUtil.fromString(value);
			}
		};
	}

	/**
	 * Prompts the user for int value
	 * 
	 * @param name The name of the dialog to show
	 * @param currentValue The current value to be displayed
	 * @param description The help text to provide
	 * @return The value selected by the user
	 */
	static public Value intValue (String name, final int currentValue, final String description) {
		return new DefaultValue(name, String.valueOf(currentValue)) {
			public void showDialog () {
				JSpinner spinner = new JSpinner(new SpinnerNumberModel(currentValue, Short.MIN_VALUE, Short.MAX_VALUE, 1));
				if (showValueDialog(spinner, description)) value = String.valueOf(spinner.getValue());
			}

			public Object getObject () {
				return Integer.valueOf(value);
			}
		};
	}

	/**
	 * Prompts the user for float value
	 * 
	 * @param name The name of the dialog to show
	 * @param currentValue The current value to be displayed
	 * @param description The help text to provide
	 * @param min The minimum value to allow
	 * @param max The maximum value to allow
	 * @return The value selected by the user
	 */
	static public Value floatValue (String name, final float currentValue, final float min, final float max,
		final String description) {
		return new DefaultValue(name, String.valueOf(currentValue)) {
			public void showDialog () {
				JSpinner spinner = new JSpinner(new SpinnerNumberModel(currentValue, min, max, 0.1f));
				if (showValueDialog(spinner, description)) value = String.valueOf(((Double)spinner.getValue()).floatValue());
			}

			public Object getObject () {
				return Float.valueOf(value);
			}
		};
	}

	/**
	 * Prompts the user for boolean value
	 * 
	 * @param name The name of the dialog to show
	 * @param currentValue The current value to be displayed
	 * @param description The help text to provide
	 * @return The value selected by the user
	 */
	static public Value booleanValue (String name, final boolean currentValue, final String description) {
		return new DefaultValue(name, String.valueOf(currentValue)) {
			public void showDialog () {
				JCheckBox checkBox = new JCheckBox();
				checkBox.setSelected(currentValue);
				if (showValueDialog(checkBox, description)) value = String.valueOf(checkBox.isSelected());
			}

			public Object getObject () {
				return Boolean.valueOf(value);
			}
		};
	}

	
	/**
	 * Prompts the user for a value that represents a fixed number of options. 
	 * All options are strings.
	 * 
	 * @param options The first array has an entry for each option. Each entry is either a String[1] that is both the display value
	 *           and actual value, or a String[2] whose first element is the display value and second element is the actual value.
	 *
	 * @param name The name of the value being prompted for
	 * @param currentValue The current value to show as default
	 * @param description The description of the value
	 * @return The value selected by the user
	 */
	static public Value optionValue (String name, final String currentValue, final String[][] options, final String description) {
		return new DefaultValue(name, currentValue.toString()) {
			public void showDialog () {
				int selectedIndex = -1;
				DefaultComboBoxModel model = new DefaultComboBoxModel();
				for (int i = 0; i < options.length; i++) {
					model.addElement(options[i][0]);
					if (getValue(i).equals(currentValue)) selectedIndex = i;
				}
				JComboBox comboBox = new JComboBox(model);
				comboBox.setSelectedIndex(selectedIndex);
				if (showValueDialog(comboBox, description)) value = getValue(comboBox.getSelectedIndex());
			}

			private String getValue (int i) {
				if (options[i].length == 1) return options[i][0];
				return options[i][1];
			}

			public String toString () {
				for (int i = 0; i < options.length; i++)
					if (getValue(i).equals(value)) return options[i][0].toString();
				return "";
			}

			public Object getObject () {
				return value;
			}
		};
	}

	/**
	 * Convers a color to a string.
	 * 
	 * @param color The color to encode to a string
	 * @return The colour as a string
	 */
	static public String toString (Color color) {
		if (color == null) throw new IllegalArgumentException("color cannot be null.");
		String r = Integer.toHexString(color.getRed());
		if (r.length() == 1) r = "0" + r;
		String g = Integer.toHexString(color.getGreen());
		if (g.length() == 1) g = "0" + g;
		String b = Integer.toHexString(color.getBlue());
		if (b.length() == 1) b = "0" + b;
		return r + g + b;
	}

	/**
	 * Converts a string to a color.
	 * 
	 * @param rgb The string encoding the colour
	 * @return The colour represented by the given encoded string
	 */
	static public Color fromString (String rgb) {
		if (rgb == null || rgb.length() != 6) return Color.white;
		return new Color(Integer.parseInt(rgb.substring(0, 2), 16), Integer.parseInt(rgb.substring(2, 4), 16), Integer.parseInt(rgb
			.substring(4, 6), 16));
	}

	/**
	 * Provides generic functionality for an effect's configurable value.
	 */
	static private abstract class DefaultValue implements Value {
		/** The value being held */
		String value;
		/** The key/name of the value */
		String name;

		/**
		 * Create a default value
		 * 
		 * @param name The name of the value being configured  
		 * @param value The value to use for the default
		 */
		public DefaultValue(String name, String value) {
			this.value = value;
			this.name = name;
		}

		/**
		 * @see org.newdawn.slick.font.effects.ConfigurableEffect.Value#setString(java.lang.String)
		 */
		public void setString(String value) {
			this.value = value;
		}

		/**
		 * @see org.newdawn.slick.font.effects.ConfigurableEffect.Value#getString()
		 */
		public String getString() {
			return value;
		}

		/**
		 * @see org.newdawn.slick.font.effects.ConfigurableEffect.Value#getName()
		 */
		public String getName() {
			return name;
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			if (value == null) {
				return "";
			}
			return value.toString();
		}

		/**
		 * Prompt the user for a value
		 * 
		 * @param component The component to use as parent for the prompting dialog
		 * @param description The description of the value being prompted for
		 * @return True if the value was configured
		 */
		public boolean showValueDialog(final JComponent component, String description) {
			ValueDialog dialog = new ValueDialog(component, name, description);
			dialog.setTitle(name);
			dialog.setLocationRelativeTo(null);
			EventQueue.invokeLater(new Runnable() {
				public void run () {
					JComponent focusComponent = component;
					if (focusComponent instanceof JSpinner)
						focusComponent = ((JSpinner.DefaultEditor)((JSpinner)component).getEditor()).getTextField();
					focusComponent.requestFocusInWindow();
				}
			});
			dialog.setVisible(true);
			return dialog.okPressed;
		}
	};

	/**
	 * Provides generic functionality for a dialog to configure a value.
	 */
	static private class ValueDialog extends JDialog {
		/** True if OK was pressed */
		public boolean okPressed = false;

		/**
		 * Create a new dialog to configure a specific value
		 * 
		 * @param component The component to use as the parent of the dialog prompting the user
		 * @param name The name of the value being configured
		 * @param description The description of the value being configured
		 */
		public ValueDialog(JComponent component, String name, String description) {
			setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			setLayout(new GridBagLayout());
			setModal(true);

			if (component instanceof JSpinner)
				((JSpinner.DefaultEditor)((JSpinner)component).getEditor()).getTextField().setColumns(4);

			JPanel descriptionPanel = new JPanel();
			descriptionPanel.setLayout(new GridBagLayout());
			getContentPane().add(
				descriptionPanel,
				new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
					0), 0, 0));
			descriptionPanel.setBackground(Color.white);
			descriptionPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
			{
				JTextArea descriptionText = new JTextArea(description);
				descriptionPanel.add(descriptionText, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
					GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
				descriptionText.setWrapStyleWord(true);
				descriptionText.setLineWrap(true);
				descriptionText.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
				descriptionText.setEditable(false);
			}

			JPanel panel = new JPanel();
			getContentPane().add(
				panel,
				new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 0,
					5), 0, 0));
			panel.add(new JLabel(name + ":"));
			panel.add(component);

			JPanel buttonPanel = new JPanel();
			getContentPane().add(
				buttonPanel,
				new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 0), 0, 0));
			{
				JButton okButton = new JButton("OK");
				buttonPanel.add(okButton);
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed (ActionEvent evt) {
						okPressed = true;
						setVisible(false);
					}
				});
			}
			{
				JButton cancelButton = new JButton("Cancel");
				buttonPanel.add(cancelButton);
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed (ActionEvent evt) {
						setVisible(false);
					}
				});
			}

			setSize(new Dimension(320, 175));
		}
	}
}
