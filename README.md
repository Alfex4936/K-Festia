# K-Festia

![image](https://github.com/user-attachments/assets/165854a2-42dc-4839-a415-0a87dc577ce8)
![example_graphql](https://github.com/user-attachments/assets/c48f0087-74fb-443e-82c6-d2d4edf144b0)
![search](https://github.com/user-attachments/assets/f2554a6c-841b-4a0e-be02-12229e02f665)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![GraphQL](https://img.shields.io/badge/GraphQL-22.x-ff69b4)](https://graphql.org/)
[![Java](https://img.shields.io/badge/Java-21%2B-blue)](https://www.oracle.com/java/)
[![OpenAI](https://img.shields.io/badge/OpenAI-API-brightgreen)](https://openai.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 🌟 Welcome to K-Festia!

K-Festia is a Spring Boot 3 application designed to manage, translate, and categorize Korean festivals seamlessly.

Leveraging the power of GraphQL, OpenAI's API,

users can effortlessly discover and explore festivals tailored to their interests.

---

## ✨ Features

- **GraphQL API**: Flexible and efficient querying of festival data.
- **Dual API Integration**: Fetch festival data from primary and secondary sources with robust fallback mechanisms.
- **Automated Translation**: Translate festival names and summaries from Korean to English using OpenAI's API. (gpt-4o-mini)
- **Intelligent Categorization**: Classify festivals into 10 predefined categories leveraging OpenAI's powerful language models.
- **Pagination & Filtering**: Easily navigate through festival listings with advanced pagination and filtering options.
- **Robust Error Handling**: Gracefully handle external API failures and data inconsistencies.
- **Search**: Powered by Apache Lucene to search Korean/English texts among festivals. (name, summary, categories, address)

---

## 🛠️ Getting Started

### Prerequisites

- **Java 21+**: Virtual Thread support required
- **Maven 4.0+**: For building the project.
- **OpenAI API Key**: translation and categorization services.
- **Redis (Optional)**: distributed caching.

## 📡 GraphQL API

### Sample Queries

1. **Fetch Festivals with Pagination**

   ```graphql
   query {
     getFestivals(month: "09", page: 0, size: 10) {
       content {
         id
         name
         summary
         nameEn
         summaryEn
         address
         naverUrl
         categories
         distance
         latitude
         longitude
       }
       pageNumber
       pageSize
       totalElements
       totalPages
     }
   }
   ```

2. **Filter Festivals by Category**

   ```graphql
   query {
     getFestivals(month: "09", page: 0, size: 5) {
       content {
         id
         name
         categories
       }
     }
   }
   ```

3. **Retrieve Specific Festival Details**

   ```graphql
   query {
     getFestivals(month: "10", page: 1, size: 5) {
       content {
         id
         name
         summary
         categories
       }
     }
   }
   ```
   
4. **Simple Router planner by walking nearby stations**

   ```graphql
   query {
     planFestivalRoute(
     startStation: "서울역",
      startDate: "2024-10-01",
      endDate: "2024-11-30",
      preferredCategories: [MUSIC_PERFORMING_ARTS, FOOD_CULINARY],
      maxFestivals: 3) {
          festivals {
          name
          startDate
          endDate
          address
          categories
          # default locale "en"
          }
        totalDistance
        totalDuration
      }
   }
   ```

> **Note**: Customize queries as per frontend requirements. Utilize GraphQL clients like [Apollo](https://www.apollographql.com/) for efficient data fetching.

[![codecov](https://codecov.io/gh/Alfex4936/K-Festia/graph/badge.svg?token=5O9FFQ3QBN)](https://codecov.io/gh/Alfex4936/K-Festia)