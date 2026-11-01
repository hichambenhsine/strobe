import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouteManager } from './route-manager';

describe('RouteManager', () => {
  let component: RouteManager;
  let fixture: ComponentFixture<RouteManager>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouteManager],
    }).compileComponents();

    fixture = TestBed.createComponent(RouteManager);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
