import express, { Request, Response } from 'express';
import bodyParser from 'body-parser';

const app = express();
const port = 3000;

app.use(bodyParser.json());

interface Version {
  versionId: string;
  version: string;
  releaseDate: string;
  downloadUrl: string;
}

let currentVersion: Version = {
  versionId: '1',
  version: '1.2.3',
  releaseDate: '2023-07-15',
  downloadUrl: 'https://example.com/downloads/1.2.3/app.zip',
};

app.get('/version', (req: Request, res: Response) => {
  res.json(currentVersion);
});

app.get('/download/:versionId', (req: Request, res: Response) => {
  const requestedVersionId = req.params.versionId;
  if (requestedVersionId === currentVersion.versionId) {
    res.redirect(currentVersion.downloadUrl);
  } else {
    res.status(404).json({ error: 'Requested version not found.' });
  }
});

app.listen(port, () => {
  console.log(`Server is running on http://localhost:${port}`);
});
