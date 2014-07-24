package com.nokia.ci.tas.commons;

import java.util.List;
import java.util.Map;

/**
 * Represent a running tas test node.
 *  
 * @author Frank Wang
 * @since Jul 31, 2012
 */
public class TestNode extends MessageItem<TestNode> {
	
	public TestNode() {
		this.setItemName( "TestNode" );
	}

	/**
	 * OS information.
	 */
	private Map<String,String> os;
	
	/**
	 * Disk information.
	 */
	private Map<String,String> disk;
	/**
	 * network information.
	 */
	private List<String> network;
	/**
	 * Memory information.
	 */
	private Map<String,String> memory;
	/**
	 * Processes information.
	 */
	private Map<Integer, String> processes;
	/**
	 * cpu information.
	 */
	private Map<String,String> cpu;

	public Map<String,String> getMemory() {
		return memory;
	}

	public void setMemory( Map<String,String> memory ) {
		this.memory = memory;
	}

	public Map<Integer, String> getProcesses() {
		return processes;
	}

	public void setProcesses( Map<Integer, String> processes ) {
		this.processes = processes;
	}

	public Map<String,String> getCpu() {
		return cpu;
	}

	public void setCpu( Map<String,String> cpu ) {
		this.cpu = cpu;
	}

	public Map<String,String> getOs() {
		return os;
	}

	public void setOs( Map<String,String> os ) {
		this.os = os;
	}

	public Map<String,String> getDisk() {
		return disk;
	}

	public void setDisk( Map<String,String> disk ) {
		this.disk = disk;
	}

	public List<String> getNetwork() {
		return network;
	}

	public void setNetwork( List<String> network ) {
		this.network = network;
	}
	
	@Override
	public void free() {
		if(disk != null)
			disk.clear();
		if(network != null)
			network.clear();
		if(processes != null)
			processes.clear();
		if(memory != null)
			memory.clear();
		if(os != null)
			os.clear();
		if(cpu != null)
			cpu.clear();
	}
}
