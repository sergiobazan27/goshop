import { Component, input } from '@angular/core';
import { OverviewMetrics } from '../../core/models/overview.model';

@Component({
  selector: 'app-service-summary',
  templateUrl: './service-summary.html',
})
export class ServiceSummary {
  readonly metrics = input.required<OverviewMetrics>();
}
