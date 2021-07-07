# allezgo

A simple service for interacting with fitness data producers and consumers.

See [allezgo-fe](https://github.com/markelliot/allezgo-fe) for a basic frontend application that uses this service.
Users looking for a hosted version can access this service via https://allezgo.io/peloton-to-garmin.

Today, the service offers only very basic functionality, with the following endpoints:

 * `PUT /api/synchronize/peloton-to-garmin`
   
   Synchronizes Peloton rides to Garmin Connect for between 1 and 30 days of history, ensuring that
   any previously synchronized rides are not duplicated. An activity in Garmin Connect that starts
   within +/- 2 minutes of a Peloton ride will be considered the same as the Peloton ride.
   
   Note that only Peloton _rides_ are synchronized, other Peloton activities are not presently supported.
   
   with a JSON body:
   * `pelotonEmail`: email used to log in to Peloton
   * `pelotonPassword`:  password used to log in to Peloton
   * `garminEmail`: email used to log in to Garmin Connect
   * `garminPassword`: password used to log in to Garmin Connect
   * `garminPelotonGearName`: the value of the "Brand & Make" field for Garmin Connect Gear corresponding to the Peloton bike
   * `numDaysToSync`: the number of days to synchronize rides for, valid for whole numbers in the range `[1, 30]`
   
   and with a JSON response (note only one of `result` or `error` will be present):
   * `result`: when credentials were valid and a synchronization was performed, a list of:
     * `activityDate`: `yyyy-MM-dd` format date of the activity
     * `title`: title of this activity
     * `descruption`: description of this activity
     * `pelotonLink`: the URL a user would use to view this activity on the Peloton website
     * `garminLink`: the URL a user would use to view this activity on Garmin Connect
     * `wasCreated`: true when the synchronize action caused this ride to be created in Garmin Connect
   * `error`: when a problem occurs, a non-null string describing the problem
   

