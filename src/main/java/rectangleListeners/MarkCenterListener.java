package rectangleListeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import listeners.RimSelectionListener;
import pluginTools.InteractiveSimpleEllipseFit;

public class MarkCenterListener implements ActionListener {
	
	
	final RimSelectionListener parent;
	final InteractiveSimpleEllipseFit grandparent;
	
	
	public MarkCenterListener(final RimSelectionListener parent, final InteractiveSimpleEllipseFit grandparent) {
		
		
		this.parent = parent;
		this.grandparent = grandparent;
		
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		
		
		Markpoint pointnew = new Markpoint(parent, grandparent);
		pointnew.markcenter();
		
		
	}

}
