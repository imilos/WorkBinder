package visnja;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ArrayOfSolutionForXML")
public class ArrayOfSolutionForXML {
	@XmlElement(name = "SolutionForXML")
	public List<SolutionForXML> solutionsForXML = new ArrayList<>();
	
	// Klasa SolutionForXML za ulaz
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "SolutionForXML")
	static class SolutionForXML {

		@XmlElement(name = "Guid")
		public String Guid;
		
		@XmlElement(name = "NumOfPar")
		public int NumOfPar;

		@XmlElementWrapper(name = "Parameters")
		@XmlElement(name = "double")
		public List<Double> Parameters = new ArrayList<>();
		
		int getNumOfPar() {
			return this.NumOfPar;
		}
	}
	
	/**
	 * Vraca niz parametara iz XMLINPUT-a
	 * 
	 * @param String xmlData
	 * @return ArrayOfSolutionForXML
	 * @throws 
	 */
	static ArrayOfSolutionForXML readInputFromXML(String xmlData) {

		ArrayOfSolutionForXML inp = new ArrayOfSolutionForXML();
		StringReader reader = new StringReader(xmlData);
		try {
			
			JAXBContext jaxbContext = JAXBContext.newInstance(ArrayOfSolutionForXML.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			//jaxbUnmarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-16");
			inp = (ArrayOfSolutionForXML) jaxbUnmarshaller.unmarshal(reader);
			
			return inp;
			
		} catch (JAXBException e) {
			e.printStackTrace();
			return null;
		}
	}


	/**
	 * Vraca niz parametara iz XMLINPUT-a, ali ako je prisutno samo jedno resenje
	 * 
	 * @param String xmlData
	 * @return SolutionForXML
	 * @throws 
	 */
	static SolutionForXML readInputFromXMLSingle(String xmlData) {

		SolutionForXML inp = new SolutionForXML();
		StringReader reader = new StringReader(xmlData);
		try {
			
			JAXBContext jaxbContext = JAXBContext.newInstance(SolutionForXML.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			//jaxbUnmarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-16");
			inp = (SolutionForXML) jaxbUnmarshaller.unmarshal(reader);
			
			return inp;
			
		} catch (JAXBException e) {
			e.printStackTrace();
			return null;
		}
	}

}
