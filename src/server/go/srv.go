package main

import (
	"github.com/gin-gonic/gin"
	"net/http"
)

type Version struct {
	VersionId   string `json:"versionId"`
	Version     string `json:"version"`
	ReleaseDate string `json:"releaseDate"`
	DownloadUrl string `json:"downloadUrl"`
}

var currentVersion = Version{
	VersionId:   "1",
	Version:     "1.2.3",
	ReleaseDate: "2023-07-15",
	DownloadUrl: "https://example.com/downloads/1.2.3/app.zip",
}

func main() {
	router := gin.Default()

	router.GET("/version", func(c *gin.Context) {
		c.JSON(http.StatusOK, currentVersion)
	})

	router.GET("/download/:versionId", func(c *gin.Context) {
		requestedVersionId := c.Param("versionId")
		if requestedVersionId == currentVersion.VersionId {
			c.Redirect(http.StatusTemporaryRedirect, currentVersion.DownloadUrl)
		} else {
			c.JSON(http.StatusNotFound, gin.H{"error": "Requested version not found"})
		}
	})

	port := ":3000"
	router.Run(port)
}
