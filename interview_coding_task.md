# Dogs API Task

## Background

We have a simple set of tables which holds all the dogs registered with a police force, 
this has some prescribed fields which may or may not be required and some fields which are enumerated values.

We need a fully functional RESTful API to interact with this data.

## Requirements

* No Authorisation is required
* RESTful API Standards
  * Consumes/Produces `application/json`
  * `POST /` to create an entity
  * `GET /` to list all entities
  * `GET /{id}` to get a single entity
  * `PUT /{id}` to update an entity
  * `DELETE /{id}` to delete an entity
  * Pagination
* Preferably Micronaut framework, but Spring is acceptable
* Try using mapstruct (https://mapstruct.org) to help with boilerplate for mapping entity <-> dto objects
* Nothing should be deleted from the database for audit purposes
* List endpoint should by default NOT return deleted entities
* All endpoints must sit under `/api/dogs`
* RESTful API to allow interaction with the dogs table
  * `GET /api/dogs/dogs` should list all the dogs in the database
* Any other RESTful APIs required to handle any variable enumerated values
* The dogs list endpoint should accept a `filter={}` query parameter which allows simple searching on
  * name
  * breed
  * supplier

## DataModel

A dog has the following attributes:

* Name
* Breed
* Supplier 
	* the breeder or kennels it came from
	* we may get more than 1 dog from the same supplier
* Badge Id
* Gender
* Birth Date
* Date Acquired
* Current Status
	* Currently possible values are In Training, In Service, Retired, Left
* Leaving Date
* Leaving Reason
	* Currently possible reasons are Transferred, Retired (Put Down), KIA, Rejected, Retired (Re-housed), Died
* Kennelling Characteristic
	* Characteristics which are important to know while the dog is in its kennel


