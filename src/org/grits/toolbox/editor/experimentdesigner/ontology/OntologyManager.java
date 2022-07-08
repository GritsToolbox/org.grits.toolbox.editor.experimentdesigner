package org.grits.toolbox.editor.experimentdesigner.ontology;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.grits.toolbox.core.dataShare.PropertyHandler;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class OntologyManager {
	
	
	public static String ontURI = "http://www.grits-toolbox.org/ontology/experimentdesigner";
	public static String baseURI = ontURI + "#";
	public static String dcTermsURI = "http://purl.org/dc/terms/";
	
	public static final String PROTOCOLCATEGORY_CLASS_URI = baseURI + "Category";
	public static final String TOPLEVELCATEGORY_CLASS_URI = baseURI + "PaletteCategory";

	public static final String PARAMETERGROUP_CLASS_URI = baseURI + "ParameterGroup";
	public static final String PARAMETER_CLASS_URI = baseURI + "Parameter";
	public static final String PROTOCOL_CLASS_URI = baseURI + "Protocol";
	public static final String PAPER_CLASS_URI = baseURI + "PaperReference";
	public static final String PARAMETERCONTEXT_CLASS_URI = baseURI + "ParameterContext";
	public static final String PARAMETERINGROUPCONTEXT_CLASS_URI = baseURI + "ParameterInParameterGroupContext";
	public static final String PARAMETER_GROUPCONTEXT_CLASS_URI = baseURI + "ParameterGroupContext";
	public static final String UNIT_URI = baseURI + "Unit";
	public static final String NAMESPACE_CLASS_URI = baseURI + "namespace";
	public static final String GUIDELINE_CLASS_URI = baseURI + "StandardGuideline";
	
	public OntModel standardOntologymodel = null;
	public OntModel localOntologymodel = null;
	
	public OntologyManager() {
	}
	
	public OntologyManager (OntModel model) {
		this.standardOntologymodel = model;
	}
	
	public OntologyManager (OntModel model, OntModel local) {
		this.standardOntologymodel = model;
		this.localOntologymodel = local;
	}
	
	public OntologyManager (InputStream standardInputOntology) {
		this.standardOntologymodel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RDFS_INF, null);
        this.standardOntologymodel.read(standardInputOntology, OntologyManager.baseURI);	
	}
	
	public OntologyManager (InputStream standardInputOntology, InputStream localInputOntology) {
		this.standardOntologymodel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RDFS_INF, null);
        this.standardOntologymodel.read(standardInputOntology, OntologyManager.baseURI);	
        this.localOntologymodel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RDFS_INF, null);
        this.localOntologymodel.read(localInputOntology, OntologyManager.baseURI);
	}
	
	public void reloadLocalOntology (InputStream localInputOntology) {
		 this.localOntologymodel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RDFS_INF, null);
	     this.localOntologymodel.read(localInputOntology, OntologyManager.baseURI);
	}
	
	public void writeOntology (OntModel model, OutputStream outputOntology){
       model.write(outputOntology);
    }
	
	public void addProperty(OntModel model, String subjectUri, String property,
            String objectUri) {
        Individual indiv = model.getIndividual(subjectUri);
        String propertyUri  = baseURI + property;
        Property prop = model.getObjectProperty(propertyUri);
        if(prop != null) {
            indiv.addProperty(prop, model.getIndividual(objectUri));
        }
    }


    public void addLiteral(OntModel model, String subjectUri, String property,
            Literal literalValue) {
        Individual indiv = model.getIndividual(subjectUri);
        String propertyUri  = baseURI + property;
        Property prop = model.getProperty(propertyUri);
        if(prop!=null) {
            indiv.addLiteral(prop, literalValue);
        }
    }
    
    public boolean addComment (OntModel model, String subjectUri, String comment) {
    	Individual indiv = getIndividual(model, subjectUri);
    	if (indiv != null) {
    		indiv.addComment(comment, null);
    		return true;
    	}
    	return false;
    }
    
    public boolean addAnnotation (OntModel model, String subjectUri, String annotationName, String value) {
    	Individual indiv = getIndividual(model, subjectUri);
    	if (indiv != null) {
    		Literal literalValue = model.createLiteral(value);
    		indiv.addLiteral(model.getProperty(dcTermsURI + annotationName), literalValue);
    		return true;
    	}
    	return false;
    }
    
    public String createNewIndividual(OntModel model,
			String classUri, String label) throws UnsupportedEncodingException {
    	 String indivURI = baseURI + URLEncoder.encode(label.replace(' ', '_'), PropertyHandler.GRITS_CHARACTER_ENCODING);
         Individual indiv = model.createIndividual(indivURI, 
                 model.getOntClass(classUri));
         indiv.setLabel(label, null);

         return indiv.getURI();
	}
    
    public void createNewIndividualwithURI(OntModel model,
			String classUri, String label, String uri) {	
    	Individual indiv = model.createIndividual(uri, 
                model.getOntClass(classUri));
        indiv.setLabel(label, null);
	}
    
    String createUniqueRandomIndividualURI(String className)
    {
        String newURI = OntologyManager.baseURI + className.toLowerCase().replaceAll(" ", "_");
        Random random = new Random();
        int randomLength = 8;
        boolean notUnique = true;
        String randomSuffix = null;
        char randamCharacter;
        Resource searchResource = null;
        while(notUnique)
        {
            randomSuffix = "";
            for(int i = 0; i< randomLength; i++)
            {
                randamCharacter = (char) (97 + random.nextInt(26));
                randomSuffix = randomSuffix + randamCharacter;
            }
            searchResource = ResourceFactory.createResource(newURI + "_" + randomSuffix);
            notUnique = standardOntologymodel.containsResource(searchResource);
            if(localOntologymodel != null)
            {
                notUnique  = notUnique || localOntologymodel.containsResource(searchResource);
            }
        }

        newURI = searchResource.getURI();

        return newURI;
    }
	
	public Individual getIndividual (String uri) {
		return getIndividual(standardOntologymodel, uri);
	}
	
	public Individual getIndividual (OntModel model, String uri) {
		return model.getIndividual(uri);
	}
	
	public List<Individual> getAllIndiviudalsOfClass(String classUri) {
		return getAllIndiviudalsOfClass(standardOntologymodel, classUri);
	}

    public List<Individual> getAllIndiviudalsOfClass(OntModel model, String classUri) {
        OntClass thisClass = model.getOntClass(classUri);
        Set<Individual> individuals = model.listIndividuals(thisClass).toSet();
        List<Individual> actualIndividuals = new ArrayList<>(individuals);

        return actualIndividuals;
    }
    
    public Set<String> getAllSubjectURIs(String propertyLabel, String objectUri) {
    	return getAllSubjectURIs(standardOntologymodel, propertyLabel, objectUri);
    }
    
    public Set<String> getAllSubjectURIs(OntModel model, String propertyLabel, String objectUri) {
        Property property = model.getProperty(baseURI + propertyLabel);
        ResIterator subjectIterator = model.listSubjectsWithProperty(property, model.getIndividual(objectUri));
        Set<String> subjects = new HashSet<String>();
        while(subjectIterator.hasNext()) {
            subjects.add(subjectIterator.next().getURI());
        }
        return subjects;
    }
    
    public Set<String> getAllIndividualURIs(OntModel model, Property property, String propertyValue) {
    	ResIterator subjectIterator = model.listResourcesWithProperty(property, propertyValue);
        Set<String> subjects = new HashSet<String>();
        while(subjectIterator.hasNext()) {
            subjects.add(subjectIterator.next().getURI());
        }
        return subjects;
    }
    
    public List<Individual> getAllSubjects(String propertyLabel, String objectUri) {
    	return getAllSubjects(standardOntologymodel, propertyLabel, objectUri);
    }
    public List<Individual> getAllSubjects(OntModel model, String propertyLabel, String objectUri) { 
        Property property = model.getProperty(baseURI + propertyLabel);
        List<Individual> subjects = new ArrayList<Individual>();
        Individual indiv = model.getIndividual(objectUri);
        if (indiv == null) {
        	// cannot check property to null
        	return subjects;
        }
        ResIterator subjectIterator = model.listSubjectsWithProperty(property, model.getIndividual(objectUri));
        Resource subject;
        while(subjectIterator.hasNext()) {
            subject = subjectIterator.next();
            if(model.getIndividual(subject.getURI())!=null) {
                subjects.add(model.getIndividual(subject.getURI()));
            }
        }
        return subjects;
    }
    public List<Individual> getAllObjects(Individual subject,
            String property) {
    	return getAllObjects(standardOntologymodel, subject, property);
    }
    
    public List<Individual> getAllObjects(OntModel model, Individual subject,
            String property) {

        NodeIterator objectIterator = model.listObjectsOfProperty(subject, model.getProperty(baseURI + property));
        List<Individual> objects = new ArrayList<Individual>();
        for(RDFNode n : objectIterator.toList()) {
            String uri = n.asResource().getURI();
            Individual indiv = model.getIndividual(uri);
            objects.add(indiv);
        }
        return objects;
    }

	public Literal getLiteralValue(Individual indiv, String propertyName) {
		return getLiteralValue(standardOntologymodel, indiv, propertyName);
	}
	public Literal getLiteralValue(OntModel model, Individual indiv, String propertyName) {
        Literal val = null;
        RDFNode node = indiv.getPropertyValue(model.getProperty(baseURI + propertyName));
        if(node!= null && node.isLiteral()) {
            val = node.asLiteral();
        }
        return val;
	}
	
	public List<Literal> getLiteralValues(Individual indiv, String propertyName) {
		return getLiteralValues(standardOntologymodel, indiv, propertyName);
	}
	public List<Literal> getLiteralValues(OntModel model, Individual indiv, String propertyName) {
        List<Literal> literals = new ArrayList<Literal>();
        NodeIterator nodeIterator = indiv.listPropertyValues(model.getProperty(baseURI + propertyName));
        RDFNode node = null;
        while(nodeIterator.hasNext()) {
            node = nodeIterator.next();
            if(node.isLiteral()) {
                literals.add(node.asLiteral());
            }
        }
        return literals;
    }
	public Literal getAnnotationValue (Individual indiv, String annotationName) {
		return getAnnotationValue(standardOntologymodel, indiv, annotationName);
	}
	public Literal getAnnotationValue (OntModel model, Individual indiv, String annotationName) {
		Literal val = null;
        RDFNode node = indiv.getPropertyValue(model.getProperty(dcTermsURI + annotationName));
        if(node!= null && node.isLiteral()) {
            val = node.asLiteral();
        }
        return val;
	}
	
	public List<Literal> getAnnotationValues(OntModel model, Individual indiv, String annotationName) {
		List<Literal> literals = new ArrayList<Literal>();
        NodeIterator nodeIterator = indiv.listPropertyValues(model.getProperty(dcTermsURI + annotationName));
        RDFNode node = null;
        while(nodeIterator.hasNext()) {
            node = nodeIterator.next();
            if(node.isLiteral()) {
                literals.add(node.asLiteral());
            }
        }
        return literals;
	}
}
