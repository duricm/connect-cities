package com.connect.cities.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.connect.cities.ConnectCitiesController;

public class CitiesConnectedTest {

	ConnectCitiesController c = new ConnectCitiesController();

	@Test
	public void testCheckIfNewarkBostonConnected() throws Exception
	{
        assertEquals(c.checkIfConnected("Newark", "Boston"), "yes");
	}
	
	@Test
	public void testCheckIfPhiladelphiaBostonConnected() throws Exception
	{
        assertEquals(c.checkIfConnected("Philadelphia", "Boston"), "yes");
	}
	
	@Test
	public void testCheckIfNewarkRichmondConnected() throws Exception
	{
        assertEquals(c.checkIfConnected("Newark", "Richmond"), "no");
	}

}
