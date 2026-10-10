import { Component } from '@angular/core';
import { LucideArmchair, LucideLayoutDashboard, LucidePackage, LucideReceiptText } from '@lucide/angular';

@Component({
  imports: [LucideArmchair, LucideLayoutDashboard, LucidePackage, LucideReceiptText],
  selector: 'app-sidebar',
  styleUrl: './sidebar.css',
  templateUrl: './sidebar.html',
})
export class Sidebar {}
