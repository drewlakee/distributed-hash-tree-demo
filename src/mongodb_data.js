db.createCollection("careers")
db.careers.insertMany(
    [
      {
        "_id": 1,
        "career": {
          "educationDegree": "Architect",
          "experience": [
            {
              "title": "Architect",
              "companyName": "SmallTowns"
            }
          ]
        },
        "name": "Andrew"
      },
      {
        "_id": 2,
        "career": {
          "educationDegree": "Engineer",
          "experience": [
            {
              "title": "Developer",
              "companyName": "ExtraSolutions"
            },
            {
              "title": "QA Engineer",
              "companyName": "QuickSoft"
            }
          ]
        },
        "name": "Michel"
      },
      {
        "_id": 3,
        "career": {
          "educationDegree": "NONE",
          "experience": [
            {
              "title": "System Administrator",
              "companyName": "MegaExp"
            }
          ]
        },
        "name": "Margaret"
      }
    ]
)