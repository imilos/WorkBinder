package visnja;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ArrayOfEvaluationResult")
class ArrayOfEvaluationResult {
	@XmlElement(name = "EvaluationResult")
	public List<EvaluationResult> evaluationResults = new ArrayList<>();
	
	// Klasa EvaluationResult za izlaz
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "EvaluationResult")
	static class EvaluationResult {

		@XmlElement(name = "Status")
		public String Status;

		@XmlElement(name = "Message")
		public String Message;

		@XmlElementWrapper(name = "Result")
		@XmlElement(name = "double")
		public List<Double> Result = new ArrayList<>();

		@XmlElement(name = "Metadata")
		public String Metadata = "";
	}
	
	/**
	 * Pakuje rezultate u XML format
	 * 
	 * @param ArrayOfEvaluationResult
	 * @return List
	 * @throws 
	 */
	static String writeResultsToXML(ArrayOfEvaluationResult res) {
		
		StringWriter xmlOutput = new StringWriter();

		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(ArrayOfEvaluationResult.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			//jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-16");
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(res, xmlOutput);
			
			return xmlOutput.toString(); 
			
		} catch (JAXBException e) {
			e.printStackTrace();
			return null;
		}
	}	
}	
