// Create the mixit database
db = db.getSiblingDB('mixitdb');

// Create a user to use this database
db.createUser({user: "mixit", pwd: "mixit23", roles : [{role: "readWrite", db: "mixitdb"}]});

// Create an empty collection. The database will be only created when a collection is added
db.createCollection('init');

