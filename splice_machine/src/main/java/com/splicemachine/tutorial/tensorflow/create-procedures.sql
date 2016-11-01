CREATE PROCEDURE SPLICE.GET_INPUT_DICTIONARY(dictionaryName VARCHAR(50), trainTable VARCHAR(50), testTable VARCHAR(50), path  VARCHAR(50))
   PARAMETER STYLE JAVA
   READS SQL DATA
   LANGUAGE JAVA
   DYNAMIC RESULT SETS 1
   EXTERNAL NAME 'com.splicemachine.tutorial.tensorflow.CreateInputDictionary.createInputDictionary';
   
   
CREATE PROCEDURE SPLICE.RUN_PYTHON(scriptPathAndName VARCHAR(300))
   PARAMETER STYLE JAVA
   READS SQL DATA
   LANGUAGE JAVA
   EXTERNAL NAME 'com.splicemachine.tutorial.tensorflow.CreateInputDictionary.callPythonScript';