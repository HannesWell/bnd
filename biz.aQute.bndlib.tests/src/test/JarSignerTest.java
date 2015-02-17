package test;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import junit.framework.*;
import aQute.bnd.osgi.*;
import aQute.bnd.signing.*;
import aQute.lib.io.*;
import aQute.libg.generics.*;

@SuppressWarnings({
		"resource", "restriction"
})
public class JarSignerTest extends TestCase {

	public static void testNoManifest() throws Exception {
		Builder b = new Builder();
		b.setProperty("-sign", "test");
		b.setProperty(Constants.PLUGIN, JartoolSigner.class.getName()
				+ ";keystore=testresources/keystore;keypass=testtest;storepass=testtest;sigfile=test");
		b.setProperty("-nomanifest", "true");
		b.setProperty("-resourceonly", "true");
		b.setProperty("Include-Resource", "WEB-INF/classes=@jar/osgi.jar");

		Jar jar = b.build();
		File tmp = new File("xyztmp.jar");
		tmp.deleteOnExit();

		jar.write(tmp);

		Jar jar2 = new Jar(tmp);
		Manifest manifest = jar2.getManifest();
		assertEquals("1.0", manifest.getMainAttributes().getValue("Manifest-Version"));
		assertNotNull(manifest.getAttributes("WEB-INF/classes/org/osgi/framework/BundleContext.class"));
	}

	public static void testError() throws Exception {
		JartoolSigner signer = new JartoolSigner();
		Map<String,String> properties = Create.map();
		properties.put("keystore", "testresources/keystore");
		properties.put("keypass", "testtest");
		properties.put("storepass", "notvalid");
		signer.setProperties(properties);

		Jar jar = new Jar(IO.getFile("testresources/test.jar"));
		Builder b = new Builder();
		b.setTrace(true);
		b.setJar(jar);
		signer.sign(b, "test");
		System.err.printf(Processor.join(b.getErrors(), "%n") + "%n");
		assertEquals(1, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
	}

	public static void testSimple() throws Exception {
		JartoolSigner signer = new JartoolSigner();
		Map<String,String> properties = Create.map();
		properties.put("keystore", "testresources/keystore");
		properties.put("keypass", "testtest");
		properties.put("storepass", "testtest");
		properties.put("sigFile", "test");
		properties.put("digestalg", "SHA1");
		signer.setProperties(properties);

		Jar jar = new Jar(IO.getFile("testresources/test.jar"));
		Set<String> names = new HashSet<String>(jar.getResources().keySet());
		names.remove("META-INF/MANIFEST.MF");
		Builder b = new Builder();
		b.setJar(jar);
		signer.sign(b, "test");
		System.err.printf(Processor.join(b.getErrors(), "%n") + "%n");
		System.err.printf(Processor.join(b.getWarnings(), "%n") + "%n");
		assertEquals(0, b.getErrors().size());
		assertEquals(0, b.getWarnings().size());
		assertNotNull(jar.getResource("META-INF/TEST.SF"));
		Manifest m = jar.getManifest();

		// Should have added 2 new resources: TEST.SF and TEST.DSA/RSA
		assertEquals(names.size(), b.getJar().getResources().size() - 3);

		Attributes a = m.getAttributes("aQute/rendezvous/DNS.class");
		assertNotNull(a);
		assertEquals("G0/1CIZlB4eIVyY8tU/ZfMCqZm4=", a.getValue("SHA1-Digest"));

		// Check if all resources are named
		for (String name : names) {
			System.err.println("name: " + name);
			assertNotNull(m.getAttributes(name));
		}
	}

}
