package com.lyleclassen.protractor.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ServerKeyTest {
	@Test
	void lowercasesAndStripsDefaultPort() {
		assertEquals("play.example.com.json", ServerKey.fileName("Play.Example.com:25565"));
	}

	@Test
	void keepsOtherPortsWithAFileSafeSeparator() {
		assertEquals("10.0.0.5_25570.json", ServerKey.fileName("10.0.0.5:25570"));
	}
}
