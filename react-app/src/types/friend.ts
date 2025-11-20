/**
 * Friend entity interface matching backend Friend model
 */
export interface Friend {
  id?: string;
  userId?: string;
  firstName: string;
  lastName: string;
  venmoHandle?: string;
  zellePhoneNumber?: string;
  paypalHandle?: string;
}
