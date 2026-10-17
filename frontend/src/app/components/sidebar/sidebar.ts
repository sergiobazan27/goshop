import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideArmchair,
  LucideLayoutDashboard,
  LucidePackage,
  LucideReceiptText,
  LucideStore,
} from '@lucide/angular';

@Component({
  imports: [
    RouterLink,
    LucideArmchair,
    LucideLayoutDashboard,
    LucidePackage,
    LucideReceiptText,
    LucideStore,
  ],
  selector: 'app-sidebar',
  styleUrl: './sidebar.css',
  templateUrl: './sidebar.html',
})
export class Sidebar {
  readonly mode = input<'business' | 'admin'>('business');
  readonly username = input('Mi cuenta');
}
