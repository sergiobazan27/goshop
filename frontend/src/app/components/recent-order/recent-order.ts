import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideArmchair } from '@lucide/angular';
import { OverviewOrder } from '../../core/models/overview.model';

@Component({
  selector: 'app-recent-order',
  imports: [DatePipe, RouterLink, LucideArmchair],
  templateUrl: './recent-order.html',
  host: { class: 'block' },
})
export class RecentOrder {
  readonly order = input.required<OverviewOrder>();
}
