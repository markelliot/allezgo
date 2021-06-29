# allezgo

A small set of tools for interacting with fitness data producers and consumers.

Today, the CLI offers only very basic functionality, with the following sub-commands:

 * `latest`: synchronizes the last 30 days of Peloton rides to Garmin Connect based on
   the credentials stored in the `~/.allezgo` configuration file. 
   
   Peloton rides are formatted to a Garmin-compatible interchange format (Training 
   Center Database/TCX), with each ride segment (warm-up, workout, cool-down)
   configured as a manual lap. 
   
   Additionally, the CLI uses the Garmin API to match the title and description to
   the Peloton class and adjusts the equipment to match the named equipment in the
   configuration file.
