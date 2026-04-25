import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';

@Component({
  selector: 'app-upload',
  templateUrl: './upload.component.html'
})
export class UploadComponent {
  selectedFile: File | null = null;

  constructor(private http: HttpClient) {}

  onFileSelected(event: any) {
    this.selectedFile = event.target.files[0];
  }

  upload() {
    if (!this.selectedFile) return;

    // Step 1: Get presigned URL from Lambda
    this.http.get<any>('YOUR_API_GATEWAY_URL')
      .subscribe(response => {
        const presignedUrl = response.url;

        // Step 2: Upload file to S3
        this.http.put(presignedUrl, this.selectedFile, {
          headers: {
            'Content-Type': 'application/pdf'
          }
        }).subscribe({
          next: () => console.log('Upload success'),
          error: err => console.error('Upload failed', err)
        });
      });
  }
}