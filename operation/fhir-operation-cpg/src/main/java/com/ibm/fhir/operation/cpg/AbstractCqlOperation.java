package com.ibm.fhir.operation.cpg;

import static com.ibm.fhir.cql.engine.model.ModelUtil.fhirstring;
import static com.ibm.fhir.cql.engine.model.ModelUtil.javastring;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.InMemoryLibraryLoader;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;

import com.ibm.fhir.core.FHIRConstants;
import com.ibm.fhir.cql.engine.converter.FhirTypeConverter;
import com.ibm.fhir.cql.engine.converter.impl.FhirTypeConverterImpl;
import com.ibm.fhir.cql.engine.model.FhirModelResolver;
import com.ibm.fhir.cql.engine.searchparam.SearchParameterResolver;
import com.ibm.fhir.cql.engine.server.retrieve.ServerFhirRetrieveProvider;
import com.ibm.fhir.cql.engine.server.terminology.ServerFhirTerminologyProvider;
import com.ibm.fhir.exception.FHIROperationException;
import com.ibm.fhir.model.resource.Library;
import com.ibm.fhir.model.resource.Parameters;
import com.ibm.fhir.model.resource.Parameters.Parameter;
import com.ibm.fhir.model.type.code.IssueType;
import com.ibm.fhir.server.operation.spi.AbstractOperation;
import com.ibm.fhir.server.operation.spi.FHIRResourceHelpers;

public abstract class AbstractCqlOperation extends AbstractOperation {
    
    private static final Logger log = Logger.getLogger(AbstractCqlOperation.class.getName());

    protected Parameters doEvaluation(FHIRResourceHelpers resourceHelper, Map<String, Parameter> paramMap, Library primaryLibrary) {
        Parameters result;
        FhirTypeConverter typeConverter = new FhirTypeConverterImpl();
        ParameterConverter parameterConverter = new ParameterConverter(typeConverter);

        LibraryLoader ll = createLibraryLoader(primaryLibrary);

        TerminologyProvider termProvider = new ServerFhirTerminologyProvider();

        SearchParameterResolver resolver = new SearchParameterResolver();
        ServerFhirRetrieveProvider retrieveProvider = new ServerFhirRetrieveProvider(resourceHelper, resolver);
        retrieveProvider.setExpandValueSets(false); // TODO - use server config settings
        retrieveProvider.setTerminologyProvider(termProvider);
        retrieveProvider.setPageSize(FHIRConstants.FHIR_PAGE_SIZE_DEFAULT_MAX); // TODO - use server config settings?

        Map<String, DataProvider> dataProviders = createDataProviders(retrieveProvider);

        VersionedIdentifier vid = new VersionedIdentifier();
        vid.withId(javastring(primaryLibrary.getName()));
        vid.withVersion(javastring(primaryLibrary.getVersion()));

        Pair<String, Object> context = getCqlContext(paramMap);

        Map<String, Object> engineParameters = getCqlParameters(parameterConverter, paramMap);

        Set<String> expressions = getCqlExpressionsToEvaluate(paramMap);

        DebugMap debugMap = new DebugMap();
        debugMap.setIsLoggingEnabled(true);
        debugMap.setIsCoverageEnabled(true);

        // TODO - add configuration support for the CQL engine's local time zone
        CqlEngine engine = new CqlEngine(ll, dataProviders, termProvider);
        EvaluationResult evaluationResult = engine.evaluate(vid, expressions, context, engineParameters, debugMap);

        Parameters.Builder output = Parameters.builder();
        for (Map.Entry<String, Object> entry : evaluationResult.expressionResults.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            try { 
                output.parameter(parameterConverter.toParameter(value).name(fhirstring(name)).build());
            } catch( NotImplementedException nex ) {
                log.warning( "Ignoring not-implemented parameter type" );
            }
        }
        result = output.build();
        return result;
    }

    protected Map<String, DataProvider> createDataProviders(RetrieveProvider retrieveProvider) {
        String suffix = "";

        // This is a hack to get around the fact that IBM FHIR types are in multiple packages
        // and the CQL engine expects there to be only a single package name
        Map<String, DataProvider> dataProviders = new HashMap<>();
        for (String packageName : FhirModelResolver.ALL_PACKAGES) {
            ModelResolver modelResolver = new FhirModelResolver();
            modelResolver.setPackageName(packageName);

            DataProvider provider = new CompositeDataProvider(modelResolver, retrieveProvider);
            dataProviders.put(Constants.FHIR_NS_URI + suffix, provider);
            suffix += "-";
        }
        return dataProviders;
    }

    protected Pair<String, Object> getCqlContext(Map<String, Parameters.Parameter> paramMap) {
        Pair<String, Object> context = null;
        Parameter subjectParam = paramMap.get("subject");
        if (subjectParam != null) {
            context = getCqlContext(context, subjectParam);
        }
        return context;
    }

    protected Pair<String, Object> getCqlContext(Pair<String, Object> context, Parameter subjectParam) {
        String ref = javastring(((com.ibm.fhir.model.type.String) subjectParam.getValue()));
        String[] parts = ref.split("/");
        if (parts.length >= 2) {
            context = Pair.of(parts[parts.length - 2], parts[parts.length - 1]);
        } else {
            throw new IllegalArgumentException(String.format("Invalid format for subject parameter '%s'. Value must contain a resource type and ID.", ref));
        }
        return context;
    }

    protected Map<String, Object> getCqlParameters(ParameterConverter converter, Map<String, Parameters.Parameter> paramMap) {
        Map<String, Object> engineParameters = null;
        Parameter parametersParam = paramMap.get("parameters");
        if (parametersParam != null) {
            engineParameters = getCqlEngineParameters(converter, parametersParam);
        }
        return engineParameters;
    }

    protected Map<String, Object> getCqlEngineParameters(ParameterConverter converter, Parameter parametersParam) {
        Map<String, Object> engineParameters;
        engineParameters = new HashMap<>();

        Parameters nested = (Parameters) parametersParam.getResource();
        for (Parameter engineParam : nested.getParameter()) {
            Object converted = converter.toCql(engineParam);
            engineParameters.put(javastring(engineParam.getName()), converted);
        }
        return engineParameters;
    }

    @SuppressWarnings("unchecked")
    protected Set<String> getCqlExpressionsToEvaluate(Map<String, Parameters.Parameter> paramMap) {
        Set<String> expressions = null;
        Parameter expressionsParam = paramMap.get("expressions");
        if (expressionsParam != null) {
            ((List<com.ibm.fhir.model.type.String>) expressionsParam).stream().map(p -> p.getValue()).collect(Collectors.toSet());
        }
        return expressions;
    }

    protected LibraryLoader createLibraryLoader(Library primaryLibrary) {
        List<org.cqframework.cql.elm.execution.Library> libraries = LibraryHelper.loadLibraries(primaryLibrary);
        return new InMemoryLibraryLoader(libraries);
    }

    protected Map<String, Parameters.Parameter> indexParametersByName(Parameters parameters) {
        Map<String, Parameters.Parameter> paramMap = parameters.getParameter().stream().collect(Collectors.toMap(p -> p.getName().getValue(), p2 -> p2));
        return paramMap;
    }

    protected Parameter getParameter(Map<String, Parameter> parameters, String paramName) {
        return parameters.get(paramName);
    }

    protected Parameter getRequiredParameter(Map<String, Parameter> parameters, String paramName) throws FHIROperationException {
        Parameter p = parameters.get(paramName);
        if (p == null) {
            String msg = "Missing required parameter " + paramName;
            throw buildExceptionWithIssue(msg, IssueType.INVALID);
        }
        return p;
    }
}
