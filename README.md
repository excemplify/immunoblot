WHAT IS EXCEMPLIFY?


Excemplify has been developed to automate the creation and handling of data sheets at different stages of an biological lab experiment. It has been built in order to facilitate the daily data management of experimentalists. This often consists of creating a specialized Excel sheet describing a stage of an experiment, then performing the experiment and finally creating a new sheet for the next stage. For this process, Excemplify creates templates based on an experimental setup and supports the import of data sets from lab instruments The intelligent parsing technology cuts down the amount of effort and reduces the chance of mistakes caused by manual data handling. The basic idea is that each experimental stage is accompanied by an Excel sheet. And each sheet depends on the sheet in previous stages. By generating final upload files in accordance to the required format, it can be easily exchanged or integrated with any database of interest.


PREREQUISITES


Excemplify is a Grails web application (see http://grails.org to learn more) running on Grails version 2.0.3 (Upgrading to higher Grails version might have problems of incompatibility and corresponding changes for configuration and upgrading grails plugins might be needed.)


SETUP


To run Excemplify locally just go to the root directory of the repository and run
        
        grails prod run-app


To deploy Excemplify to a servlet container like Tomcat you can create a war-file by running


        grails war


and then deploy the resulting target/excemplify.war to your servlet container.


USAGE


A predefined admin account and a normal experimentalist account are set in the bootstrapping process. You can use them for login to test other features.
In addition the database is prepopulated with some data to allow you to test the environment.


The portal for "New Experiment" contains all features for our project purpose. The portal for "Performed Experiment" provides features for experimentalists to store their previous existing data for transition.
AUTHORS


Code was authored by Dr. Lenneke Jong and Lei Shi <firstname.lastname@h-its.org>
Biodata expert Dr. Ulrike Wittig


PD Dr. Wolfgang Müller, Prof. Dr. Ursula Klingmüller (project proposal & supervision)


ACKNOWLEDGEMENTS


Thanks go to Dr. Lorenza d’Allesandro, Stefanie Müller, Julie Bachmann, Markus Stepath and other people from the Klingmüller Department at DKFZ.


FUNDING


Excemplify was funded by the German Research Foundation (DFG, http://dfg.de/) virtual research environment program (short name: Integrierte Immunoblot Umgebung).


Copyright holder is HITS gGmbH (http://www.h-its.org/) 2010-2013.




Last update: March.21st. 2013




